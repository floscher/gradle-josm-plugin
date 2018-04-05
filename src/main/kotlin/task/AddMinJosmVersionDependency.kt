package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.openstreetmap.josm.gradle.plugin.getNextJosmVersion
import org.openstreetmap.josm.gradle.plugin.josm

open class AddMinJosmVersionDependency: DefaultTask() {
  @Internal
  lateinit var configuration: Configuration
  init {
    doFirst {
      // Find the next available version from the one specified in the manifest
      project.dependencies.add(configuration.name, project.getNextJosmVersion(project.extensions.josm.manifest.minJosmVersion))
    }
  }
  fun init(parentConfiguration: Configuration) {
    this.configuration = project.configurations.create("minJosmVersion${parentConfiguration.name.capitalize()}").extendsFrom(parentConfiguration)
    description = "Adds dependency for the minimum required JOSM version to the configuration `${configuration.name}`."
  }
}
