package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.getAllRequiredJosmPlugins
import org.openstreetmap.josm.gradle.plugin.util.josm

/**
 * Configure this [Configuration] as the main one:
 * * derive `packIntoJar` and `requiredPlugin` configurations from this one
 * * add the JOSM dependency
 * @param [project] the project to which the configuration belongs
 */
fun Configuration.setupAsMainConfiguration(project: Project) {
  // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
  val requiredPluginConfiguration = project.configurations.create("requiredPlugin") {
    this.extendsFrom(it)
  }
  // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
  extendsFrom(project.configurations.create("packIntoJar"))

  project.afterEvaluate {
    val josmCompileVersion = project.extensions.josm.josmCompileVersion ?: throw GradleException("JOSM compile version not set!")

    // Adding dependencies for JOSM and the required plugins
    val josmDependency = project.dependencies.createJosm(josmCompileVersion)
    if (josmDependency.isChanging) {
      project.logger.info("Compile against the variable JOSM version $josmCompileVersion")
    } else {
      project.logger.info("Compile against the JOSM version ${josmCompileVersion.toInt()}")
    }
    dependencies.add(josmDependency)

    // Add dependencies on all required plugins to the `requiredPlugin` configuration
    project
      .getAllRequiredJosmPlugins(project.extensions.josm.manifest.pluginDependencies)
      .forEach {
        requiredPluginConfiguration.dependencies.add(it)
      }
  }
}
