package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.shortenPoFile
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
    inputs.files(sourceSet.po)
    outputs.files(sourceSet.po)
  }

  override fun getDescription(): String? =
    "Remove the paths from the *.po files to where a string can be found in the source code of source set `${sourceSet.name}`. " +
    "Also replaces placeholders in the *.po header."

  override fun setDescription(description: String?) =
    throw UnsupportedOperationException("Can't change description of ${javaClass.name}")

  @TaskAction
  fun action() {
    // Go through all *.po files in the "po source sets"
    sourceSet.po.files.filter { it.extension == "po" }.forEach { file ->
      logger.lifecycle("Shorten $file")
      // Read the original *.po file, shorten it, and write it back to the same file
      file.writeText(
        file.readLines().shortenPoFile(
          "Translations for the JOSM plugin '${project.extensions.josm.pluginName}' (${file.nameWithoutExtension})",
          project.extensions.josm.i18n.copyrightHolder ?: "",
          "josm-plugin_${project.extensions.josm.pluginName}"
        )
      )
    }
  }
}
