package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency

fun Configuration.setupAsMainConfiguration(project: Project) {
  // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
  val requiredPluginConfiguration = extendsFrom(project.configurations.create("requiredPlugin"))
  // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
  extendsFrom(project.configurations.create("packIntoJar"))

  project.afterEvaluate {
    val josmCompileVersion = project.extensions.josm.josmCompileVersion

    // Adding dependencies for JOSM and the required plugins
    val josmDependency = project.dependencies.create("org.openstreetmap.josm:josm:${josmCompileVersion}")
    when (josmCompileVersion) {
      "latest", "tested" -> {
        project.logger.info("Compile against the variable JOSM version ${josmCompileVersion}")
        (josmDependency as ExternalModuleDependency).setChanging(true)
      }
      else -> {
        project.logger.info("Compile against the JOSM version ${josmCompileVersion}")
      }
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
