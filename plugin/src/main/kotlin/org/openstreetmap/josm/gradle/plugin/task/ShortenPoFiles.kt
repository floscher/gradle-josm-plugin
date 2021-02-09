package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.util.josm
import javax.inject.Inject

/**
 * Shortens the *.po files in the given source set.
 *
 * This means all occurences of the source code locations of the translated strings are stripped, as well as the
 * last translator of the *.po file.
 * Also some fields in the file header are filled out (package name, copyright holder and a descriptive title).
 *
 * This task should be run after downloading fresh translations from Transifex (e.g. with [TransifexDownload]).
 *
 * @property sourceSet The source set for which this task will shorten the *.po files
 */
open class ShortenPoFiles @Inject constructor(private val sourceSet: I18nSourceSet): DefaultTask() {

  init {
    group = "JOSM-i18n"
    project.afterEvaluate {
      description = "Remove the paths to where a string can be found in the source code from the *.po files of source set `${sourceSet.name}`. Also replaces placeholders in the *.po header."
    }
    inputs.files(sourceSet.po)
    outputs.files(sourceSet.po)
  }

  @TaskAction
  fun action() {
    // Go through all *.po files in the "po source sets"
    sourceSet.po.files.filter { it.extension == "po" }.forEach { file ->
      logger.lifecycle("Shorten $file")
      val content = StringBuilder()
      var isHeader = true
      // Read the original *.po file and decide which parts to keep
      file.readLines().forEach { line ->
        var modifiedLine = line
        if (isHeader) {
          // Rewrite the generic parts of the file header
          if (modifiedLine.startsWith("# ") || modifiedLine == "#") {
            val projectName = project.extensions.josm.pluginName
            modifiedLine = modifiedLine.replace("SOME DESCRIPTIVE TITLE.", "Translations for the JOSM plugin '$projectName' (${file.nameWithoutExtension})")
            modifiedLine = modifiedLine.replace("THE PACKAGE'S COPYRIGHT HOLDER", project.extensions.josm.i18n.copyrightHolder ?: "")
            modifiedLine = modifiedLine.replace("PACKAGE package", "josm-plugin_$projectName package")
            modifiedLine = modifiedLine.replace(Regex("<[^<]+@[^>]+>"), "")
          } else {
            isHeader = false
          }
        }
        // Write all lines to the temporary file except the ones containing pointers to the source code or the last translator
        if (
          !modifiedLine.startsWith("#, ")
          && !modifiedLine.startsWith("#: ")
          && !modifiedLine.startsWith("\"Last-Translator:")
        ) {
          content.append(modifiedLine.trimEnd()).append('\n')
        }
      }
      // Overwrite the *.po file with the shortened content
      file.writeText(content.toString())
    }
  }
}
