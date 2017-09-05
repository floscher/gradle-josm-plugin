package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import org.openstreetmap.josm.gradle.plugin.setup.BasicTaskSetup
import org.openstreetmap.josm.gradle.plugin.setup.MinJosmVersionSetup
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup

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

    new BasicTaskSetup(pro: project).setup()
    if (project.josm.isPlugin) {
      new PluginTaskSetup(pro: project).setup()
    }
    new MinJosmVersionSetup(pro: project).setup()
  }
}
