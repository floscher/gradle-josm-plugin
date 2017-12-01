package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import org.openstreetmap.josm.gradle.plugin.setup.BasicTaskSetup
import org.openstreetmap.josm.gradle.plugin.setup.MinJosmVersionSetup
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup

import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.jar.Manifest

/**
 * Main class of the plugin, sets up the {@code requiredPlugin} configuration,
 * the additional repositories and the custom tasks.
 */
class JosmPlugin implements Plugin<Project> {
  private static Project currentProject;

  public static Project getCurrentProject() {
    if (currentProject == null) {
      throw new IllegalStateException("Currently the gradle-josm-plugin is not applied to a Gradle project, but you want to access the project to which the gradle-josm-plugin is applied. This should not happen ;).")
    }
    return currentProject
  }

  /**
   * Set up the JOSM plugin.
   * Creates the tasks this plugin provides, defines the {@code josm} extension, adds the repositories where JOSM specific dependencies can be found.
   * @see {@link Plugin#apply(T)}
   */
  synchronized void apply(final Project project) {
    JosmPlugin.currentProject = project;

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
      requirePlugins(project, project.josm.manifest.pluginDependencies.toArray(new String[0]))

      project.jar {
        from project.configurations.packIntoJar.collect { it.isDirectory() ? it : project.zipTree(it) }
      }
    }

    new BasicTaskSetup().setup()
    if (project.josm.isPlugin) {
      new PluginTaskSetup().setup()
    }
    new MinJosmVersionSetup().setup()
    JosmPlugin.currentProject = null;
  }

  /**
   * Convenience method
   */
  private void requirePlugins(Project pro, String... pluginNames) {
    requirePlugins(0, pro, pluginNames)
  }
  /**
   * Recursively add the required plugins and any plugins that these in turn require to the configuration `requiredPlugin`
   */
  private void requirePlugins(int recursionDepth, Project pro, String... pluginNames) {
    if (recursionDepth >= 10) {
      throw new GradleException("Dependency tree of required JOSM plugins is too deep. Aborting resolution of required JOSM plugins.")
    }
    pluginNames.each({ pluginName ->
      pluginName = pluginName.trim()
      pro.logger.info "Add required JOSM plugin '{}' to classpath…", pluginName

      final def tmpConf = pro.configurations.create('tmpConf'+recursionDepth)
      final def pluginDep = pro.dependencies.add('tmpConf'+recursionDepth, 'org.openstreetmap.josm.plugins:' + pluginName + ':', {changing = true})
      if (pro.configurations.requiredPlugin.dependencies.contains(pluginDep)) {
        pro.logger.info "JOSM plugin '{}' is already on the classpath.", pluginName
      } else {
        tmpConf.fileCollection(pluginDep).files.each { jarFile ->
          def zipFile = new ZipFile(jarFile)
          def zipEntries = zipFile.entries()
          while (zipEntries.hasMoreElements()) {
            def zipEntry = zipEntries.nextElement()
            if ('META-INF/MANIFEST.MF'.equals(zipEntry.name)) {
              def requirements = new Manifest(zipFile.getInputStream(zipEntry)).mainAttributes.getValue("Plugin-Requires")
              if (requirements != null) {
                // If the plugin itself requires more plugins, recursively add them too.
                requirePlugins(recursionDepth + 1, pro, requirements.split(';'))
              }
            }
          }
        }
        pro.configurations.requiredPlugin.dependencies.add(pluginDep)
      }
      pro.configurations.remove(tmpConf)
    })
  }
}
