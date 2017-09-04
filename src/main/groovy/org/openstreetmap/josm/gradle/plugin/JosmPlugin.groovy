package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskExecutionException

/**
 * Main class of the plugin, sets up the {@code requiredPlugin} configuration,
 * the additional repositories and the custom tasks.
 */
class JosmPlugin implements Plugin<Project> {
  /**
   * Set up the JOSM plugin.
   * Creates the tasks this plugin provides, defines the {@code josm} extension, adds the repositories where JOSM specific dependencies can be found.
   * @see {@link Plugin#apply(T)}
   */
  void apply(Project project) {
    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.plugins.findPlugin(JavaPlugin) == null) {
      project.apply plugin: 'java'
    }
    // Define 'josm' extension
    JosmPluginExtension.project = project
    project.extensions.create("josm", JosmPluginExtension)

    if (project.josm.isPlugin) {
      project.configurations {
        // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
        implementation.extendsFrom(requiredPlugin)
        // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
        implementation.extendsFrom(packIntoJar)
      }
    }

    project.repositories(project.josm.repositories)

    project.gradle.projectsEvaluated {
      project.logger.info '\n\n'
      project.logger.info "By default you'll compile against JOSM version "+project.josm.josmCompileVersion
      project.jar.manifest.attributes project.josm.manifest.createJosmPluginJarManifest()
      project.logger.info '\n\n'

      // Adding dependencies for JOSM and the required plugins
      project.dependencies.add('implementation', 'org.openstreetmap.josm:josm:'+project.josm.josmCompileVersion)
      project.josm.manifest.pluginDependencies.each({ item ->
        project.dependencies.add('requiredPlugin', 'org.openstreetmap.josm.plugins:'+item+':', {changing = true})
      })

      project.jar {
        from project.configurations.packIntoJar.collect { it.isDirectory() ? it : project.zipTree(it) }
      }
    }

    setupBasicTasks(project)
    if (project.josm.isPlugin) {
      setupPluginTasks(project)
    }
    new MinJosmVersionSetup(project).setup()
  }

  private static void setupBasicTasks(final Project project) {
    // Clean JOSM
    project.task(
      [type: Delete, group: 'JOSM', description: 'Delete JOSM configuration in `build/.josm/`'],
      'cleanJosm',
      {t ->
        project.gradle.projectsEvaluated {
          delete project.josm.tmpJosmHome
        }
        doFirst {
          project.logger.lifecycle 'Delete {}…', delete
        }
      }
    )
    // Init JOSM preferences
    project.task(
      [type: Copy, description: 'Puts a default preferences.xml file into the temporary JOSM home directory'],
      'initJosmPrefs',
      {t ->
        project.gradle.projectsEvaluated {
          from "${project.josm.josmConfigDir}"
          into "${project.josm.tmpJosmHome}"
          include 'preferences.xml'
          if (source.size() <= 0) {
            project.logger.lifecycle "No default JOSM preference file found in ${project.josm.josmConfigDir}/preferences.xml."
          }
        }
        doFirst {
          if (new File("${destinationDir}/preferences.xml").exists()) {
            project.logger.lifecycle "JOSM preferences not copied, file is already present.\nIf you want to replace it, run the task 'cleanJosm' additionally."
            return 0
          }
          project.logger.lifecycle 'Copy {} to {}…', source.files, destinationDir
        }
      }
    )
    // "Virtual task" that depends on all tasks, which put the desired plugin *.jar files into the plugins directory
    // All RunJosmTasks by default depend on this task.
    project.task(
      [
        description: 'Put all needed plugin *.jar files into the plugins directory. This task itself does nothing, but all tasks that copy the needed files (should) be set as dependencies of this task.'
      ],
      'updateJosmPlugins'
    )
    // Standard run-task
    project.task(
      [
        type: RunJosmTask.class,
        description: 'Runs an independent JOSM instance (version specified in project dependencies) with `build/.josm/` as home directory and the freshly compiled Mapillary plugin active.'
      ],
      'runJosm'
    )
    // Debug task
    project.task(
      [type: RunJosmTask.class],
      'debugJosm', { t->
        project.gradle.projectsEvaluated {
          description 'Runs a JOSM instance like the task `runJosm`, but with JDWP (Java debug wire protocol) active' + (
            project.josm.debugPort == null
            ? ".\n  NOTE: Currently the `debugJosm` task will error out! Set the property `project.josm.debugPort` to enable it!"
            : ' on port ' + project.josm.debugPort
          )
          extraInformation '\nThe application is listening for a remote debugging connection on port ' + project.josm.debugPort + '. It will start execution as soon as the debugger is connected.\n'
          jvmArgs "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + project.josm.debugPort
        }
        doFirst {
          if (project.josm.debugPort == null) {
            throw new TaskExecutionException(t, new NullPointerException(
              "You have to set the property `project.josm.debugPort` to the port on which you'll listen for debug output. If you don't want to debug, simply use the task `runJosm` instead of `debugJosm`."
            ));
          }
        }
      }
    )
  }

  private static void setupPluginTasks(final Project project) {
    project.task(
      [type: Copy],
      'updatePluginDependencies',
      {t ->
        doFirst {
          project.logger.lifecycle 'Copy {} to {}…', source.files, destinationDir
        }
        project.gradle.projectsEvaluated {
          from project.configurations.requiredPlugin
          into "${project.josm.tmpJosmHome}/plugins"
          rename('(.*)-\\.jar', '$1.jar')
        }
      }
    )
    project.task(
      [type: Copy, description: 'Puts the plugin-JAR generated by the `jar`-task into `build/.josm/`', dependsOn: [project.tasks.jar, project.initJosmPrefs, project.updatePluginDependencies]],
      'updateCurrentPlugin',
      {t ->
        doFirst {
          project.logger.lifecycle 'Copy {} to {}…', source.files, destinationDir
        }
        project.updateJosmPlugins.dependsOn t
        project.gradle.projectsEvaluated {
          from project.jar.outputs
          into "${project.josm.tmpJosmHome}/plugins"
          rename('.*', project.josm.jarName ? project.josm.jarName : '$0')
        }
      }
    )
  }
}
