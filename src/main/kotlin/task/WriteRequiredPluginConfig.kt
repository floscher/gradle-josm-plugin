package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.io.IOException

open class WriteRequiredPluginConfig : DefaultTask() {
  @OutputFile
  val destinationFile: File = File(project.buildDir, "josm-custom-config/requiredPlugins.xml")

  @Input
  val template = WriteRequiredPluginConfig::class.java.getResourceAsStream("/requiredPluginConfigTemplate.xml")
    .bufferedReader(Charsets.UTF_8)
    .use { it.readText() }

  val pluginName by lazy { project.extensions.josm.pluginName }

  @Internal
  lateinit var requiredPluginConfig: Configuration

  init {
    description = "Creates the configuration that tells JOSM which plugins to load (which is later automatically loaded by e.g. `runJosm`)"
    project.afterEvaluate {
      requiredPluginConfig = project.configurations.getByName("requiredPlugin")
      inputs.files(requiredPluginConfig)
      inputs.property("pluginName", pluginName)
    }

    doFirst {
      if (!destinationFile.parentFile.exists() && !destinationFile.parentFile.mkdirs()) {
        throw TaskExecutionException(this, IOException("Can't create missing directory ${destinationFile.parentFile.absolutePath}"))
      }
      logger.lifecycle("Write required plugin config to {}…", destinationFile.absolutePath)

      val pluginListEntries = requiredPluginConfig.dependencies
        .map { it.name }
        .plus(pluginName)
        .joinToString("\n      ") { "<entry value=\"$it\"/>" }

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
