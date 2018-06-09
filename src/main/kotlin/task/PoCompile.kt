package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
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
        .filter { it.isFile && it.name.endsWith(".mo") }
        .forEach { it.delete() }
      sourceSet.po.asFileTree.forEach {
        val commandLine = listOf(
          "msgfmt",
          "--output-file=" + File(outDir, it.nameWithoutExtension + ".mo").absolutePath,
          "--statistics",
          it.absolutePath
        )
        this.logger.lifecycle(commandLine.joinToString("  "))
        try {
          val process: Process = ProcessBuilder(commandLine).redirectErrorStream(true).start()
          if (process.waitFor(2, TimeUnit.MINUTES)) {
            this.logger.lifecycle(" > " + process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText())
          } else {
            val sourceFilePath = it.absolutePath
            logger.warn("WARNING: msgfmt takes longer than 2 minutes to convert $sourceFilePath . Aborting now!")
            project.gradle.buildFinished {
              logger.warn("WARNING: Gradle wasn't able to convert $sourceFilePath to a *.mo file in less than two minutes! I18n is incomplete!")
            }
          }
        } catch (e: IOException) {
          logger.warn("Failed to convert *.po file to *.mo file. Probably xgettext is not installed on your machine!")
          project.gradle.buildFinished {
            project.logger.error(
              "WARNING: Not all i18n files have been built! Some *.po files could not be converted to *.mo files!\nWARNING: Maybe the program xgettext is not installed on your machine! It is required for processing *.po files."
            )
          }
        }
      }
    }
  }
}

