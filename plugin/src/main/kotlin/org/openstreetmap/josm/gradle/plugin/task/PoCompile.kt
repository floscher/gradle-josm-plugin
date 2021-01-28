package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFileEncoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.encodeToLangFiles

/**
 * Compile *.po files (textual gettext files) to *.mo files (binary gettext files).
 *
 * This task requires the command line tool `msgfmt` (part of GNU gettext) to work properly! If the tool is not
 * installed, it will only issue a warning (not fail), but translations from *.po files won't be available.
 *
 * @property sourceSet The source set, for which all *.po files will be compiled to *.mo files.
 */
open class PoCompile @Inject constructor(@Internal val sourceSet: I18nSourceSet): DefaultTask() {

  @InputFiles
  val poSourceFiles = sourceSet.po

  /**
   * The target directory where the MO files will be placed (automatically initialized after the project is evaluated)
   */
  @OutputDirectory
  lateinit var outDir: File

  init {
    project.afterEvaluate {
      outDir = File(project.buildDir, "i18n/po/" + sourceSet.name)
      description = "Compile the *.po text files of source set `${sourceSet.name}` to the binary *.mo files"
    }
  }

  /**
   * The main action of this task. Compiles the *.po files in the [sourceSet] to MO files in the [outDir].
   */
  @TaskAction
  fun action() {
    val langOutDir = outDir.resolve("data")
    langOutDir.mkdirs()

    project.fileTree(langOutDir)
      .filter { it.isFile && it.extension == "lang" }
      .forEach { it.delete() }

    if (sourceSet.po.isEmpty) {
      this.logger.lifecycle("No *.po files found for this source set '{}'.", sourceSet.name)
    } else {
      val sourceFiles = sourceSet.po.asFileTree.files

      val duplicateLanguages = sourceFiles.groupBy { it.nameWithoutExtension.toLowerCase(Locale.ROOT) }.filter { it.value.size > 1 }.map { it.value }
      if (duplicateLanguages.isNotEmpty()) {
        throw TaskExecutionException(this, GradleException("There are multiple *.po files with the same name:\n" + duplicateLanguages.joinToString("\n") { "  * ${it.joinToString(", ") { file -> file.absolutePath} }" }))
      }

      logger.lifecycle("Converting ${sourceSet.po.asFileTree.files.size} *.po files to *.lang files:")
      sourceSet.po.asFileTree.files.map { file ->
        logger.lifecycle("  from ${file.absolutePath}")
        file.nameWithoutExtension to PoFileDecoder.decodeToTranslations(file.readText())
      }.toMap().let {
        encodeToLangFiles(it, langOutDir, "en")
      }
      logger.lifecycle("  into ${outDir.absolutePath}")
    }
  }
}

