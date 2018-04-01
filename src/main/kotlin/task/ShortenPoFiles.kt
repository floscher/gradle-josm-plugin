package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePluginConvention
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.josm
import java.nio.charset.StandardCharsets
import java.nio.file.Files

open class ShortenPoFiles : DefaultTask() {
  lateinit var sourceSet: I18nSourceSet

  init {
    group = "JOSM-i18n"
    project.afterEvaluate {
      description = "Remove the paths to where a string can be found in the source code from the *.po files of source set '${sourceSet.name}'. Also replaces placeholders in the *.po header."
    }
    doFirst {
      // Go through all *.po files in the "po source sets"
      sourceSet.po.files.filter { it.extension == "po" }.forEach { file ->
        logger.lifecycle("Shorten " + file)
        // Create a temporary file for the current *.po file
        val tmpFile = Files.createTempFile(null, null).toFile()
        tmpFile.bufferedWriter(StandardCharsets.UTF_8).use { out ->
          var isHeader = true
          // Read the original *.po file and decide which parts to write to the temporary file
          file.reader(StandardCharsets.UTF_8).forEachLine { line ->
            var modifiedLine = line
            if (isHeader) {
              // Rewrite the generic parts of the file header
              if (modifiedLine.startsWith("# ")) {
                val projectName = project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName
                modifiedLine = modifiedLine.replace("SOME DESCRIPTIVE TITLE.", "Translations for the JOSM plugin '$projectName' (${file.nameWithoutExtension})")
                modifiedLine = modifiedLine.replace("THE PACKAGE'S COPYRIGHT HOLDER", project.extensions.josm.i18n.copyrightHolder ?: "")
                modifiedLine = modifiedLine.replace("PACKAGE package", "josm-plugin_$projectName package")
              } else {
                isHeader = false
              }
            }
            // Write all lines to the temporary file except the ones containing pointers to the source code or the last translator
            if (!modifiedLine.startsWith("#: ") && !modifiedLine.startsWith("\"Last-Translator:")) {
              out.write(modifiedLine)
              out.newLine()
            }
          }
        }
        // Copy the temporary file to its original location and then delete the temporary file
        project.copy { it.from(tmpFile); it.into(file.parent); it.rename { file.name } }
        tmpFile.delete()
      }
    }
  }
}
