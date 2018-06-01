package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File
import java.io.IOException

open class WriteRequiredPluginConfig : DefaultTask() {
  @Internal
  val destinationFile = File(project.buildDir, "josm-custom-config/requiredPlugins.xml")

  @Internal
  val template = WriteRequiredPluginConfig::class.java.getResourceAsStream("/requiredPluginConfigTemplate.xml")
    .bufferedReader(Charsets.UTF_8)
    .use { it.readText() }

  @Internal
  lateinit var requiredPluginConfig: Configuration

  init {
    description = "Creates the configuration that tells JOSM which plugins to load (which is later automatically loaded by e.g. `runJosm`)"

    inputs.property("template", template)
    outputs.file(destinationFile)
    project.afterEvaluate {
      requiredPluginConfig = project.configurations.getByName("requiredPlugin")
      inputs.files(requiredPluginConfig)
    }

    doFirst {
      if (!destinationFile.parentFile.exists() && !destinationFile.parentFile.mkdirs()) {
        throw TaskExecutionException(this, IOException("Can't create missing directory ${destinationFile.parentFile.absolutePath}"))
      }
      logger.lifecycle("Write required plugin config to {}…", destinationFile.absolutePath)

      val pluginListEntries = requiredPluginConfig.dependencies
        .map { it.name }
        .plus(project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName)
        .map { "<entry value=\"$it\"/>" }
        .joinToString("\n      ")

      destinationFile.bufferedWriter(Charsets.UTF_8).use {
        it.write(
          template
            .replace("{{{PLUGIN_LIST_ENTRIES}}}", pluginListEntries)
            .replace("{{{tmpJosmPrefDir}}}", project.extensions.josm.tmpJosmPrefDir.absolutePath)
        )
      }
    }
  }
}
