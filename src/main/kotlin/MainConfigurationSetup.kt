package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.getAllRequiredJosmPlugins
import org.openstreetmap.josm.gradle.plugin.util.josm

/**
 * Configure the given [Configuration] as the main one:
 * * derive `packIntoJar` and `requiredPlugin` configurations from this one
 * * add the JOSM dependency
 * * add the required plugins as dependencies
 * @param [project] the project to which the configuration belongs
 * @param [configuration] the main configuration
 */

class MainConfigurationSetup(val project: Project, val mainSourceSet: SourceSet) {

  val mainConfiguration = project.configurations.getByName(mainSourceSet.implementationConfigurationName)

  // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
  val requiredPluginConfiguration = project.configurations.create("requiredPlugin") {
    mainConfiguration.extendsFrom(it)
  }

  // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
  val packIntoJarConfiguration = mainConfiguration.extendsFrom(project.configurations.create("packIntoJar"))

  /**
   * This part is meant to be called inside of an [Project.afterEvaluate] block.
   * It adds the dependencies to JOSM and required JOSM plugins to the correct configurations
   */
  fun afterEvaluate() {
    val josmCompileVersion = project.extensions.josm.josmCompileVersion ?: throw GradleException("JOSM compile version not set!")

    // Adding dependencies for JOSM and the required plugins
    mainConfiguration.dependencies.add(
      project.dependencies.createJosm(josmCompileVersion).also {
        project.logger.info(
          if (it.isChanging) "Compile against the variable JOSM version $josmCompileVersion"
          else "Compile against the JOSM version ${josmCompileVersion.toInt()}"
        )
      }
    )

    // Add dependencies on all required plugins to the `requiredPlugin` configuration
    project
      .getAllRequiredJosmPlugins(project.extensions.josm.manifest.pluginDependencies)
      .forEach {
        requiredPluginConfiguration.dependencies.add(it)
      }
  }
}
