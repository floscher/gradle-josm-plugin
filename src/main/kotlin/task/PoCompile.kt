package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Compile *.po files (textual gettext files) to *.mo files (binary gettext files).
 *
 * This task requires the command line tool `msgfmt` (part of GNU gettext) to work properly! If the tool is not
 * installed, it will only issue a warning (not fail), but translations from *.po files won't be available.
 */
open class PoCompile
  /**
   * @property sourceSet
   * The source set, for which all *.po files will be compiled to *.mo files.
   */
  @Inject
  constructor(@Internal val sourceSet: I18nSourceSet): DefaultTask() {

  private lateinit var outDir: File

  init {
    project.afterEvaluate {
      outDir = File(project.buildDir, "i18n/po/" + sourceSet.name)

      inputs.files(sourceSet.po)
      outputs.dir(outDir)

      description = "Compile the *.po text files of source set `${sourceSet.name}` to the binary *.mo files"
    }
  }

  @TaskAction
  fun action() {
    outDir.mkdirs();

    if (sourceSet.po.isEmpty) {
      this.logger.lifecycle("No *.po files found for this source set '{}'.", sourceSet.name)
    } else {
      project.fileTree(outDir)
        .filter { it.isFile && it.extension == "mo" }
        .forEach { it.delete() }


      logger.lifecycle("Converting *.po files to *.mo files using the program `msgfmt` (part of xgettext)…")
      val convertedFiles = mutableListOf<File>()
      sourceSet.po.asFileTree.forEach {
        val commandLine = listOf(
          "msgfmt",
          "--output-file=" + File(outDir, it.nameWithoutExtension + ".mo").absolutePath,
          it.absolutePath
        )
        this.logger.info("Executing command: " + commandLine.joinToString("  "))
        try {
          val process: Process = ProcessBuilder(commandLine).redirectErrorStream(true).start()
          if (process.waitFor(2, TimeUnit.MINUTES)) {
            convertedFiles.add(it)
          } else {
            val sourceFilePath = it.absolutePath
            logger.warn("WARNING: msgfmt could not convert $sourceFilePath within 2 minutes. Aborting now!")
            project.gradle.buildFinished {
              logger.error("WARNING: Gradle wasn't able to convert $sourceFilePath to a *.mo file in less than two minutes! I18n is incomplete!")
            }
          }
        } catch (e: IOException) {
          logger.warn("WARNING: Failed to convert *.po file to *.mo file. Probably xgettext is not installed on your machine!")
          project.gradle.buildFinished {
            project.logger.error(
              "WARNING: Not all i18n files have been built! Some *.po files could not be converted to *.mo files!\nWARNING: Maybe the program xgettext is not installed on your machine! It is required for processing *.po files."
            )
          }
        }
      }
      convertedFiles.groupBy { it.parentFile.absolutePath }.forEach { dir, files ->
        logger.lifecycle("  from $dir :\n    ${files.map { it.nameWithoutExtension }.sorted().joinToString(", ")}")
      }
      logger.lifecycle("  into $outDir")

      val duplicateFiles = convertedFiles.groupBy { it.nameWithoutExtension }.filter { it.value.size > 1 }
      if (duplicateFiles.isNotEmpty()) {
        throw TaskExecutionException(this, GradleException("There are multiple *.po files with the same name:\n" + duplicateFiles.map { "* ${it.key}: ${it.value.joinToString(", ")}" }.joinToString("\n")))
      }
    }
  }
}

