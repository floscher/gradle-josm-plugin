package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

open class PoCompile: DefaultTask() {
  @OutputDirectory
  lateinit var outDir: File
    private set

  @InputFiles
  lateinit var source: SourceDirectorySet
  private lateinit var sourceSetName: String

  private var compilationFailed: Boolean = false

  fun setup(sourceSet: I18nSourceSet) {
    this.outDir = File(project.buildDir, "i18n/po/" + sourceSet.name)
    this.source = sourceSet.po
    this.sourceSetName = sourceSet.name
    description = "Compile the *.po text files of source set ${sourceSet.name} to the binary *.mo files"
  }

  init {
    doFirst {
      outDir.mkdirs();

      if (source.isEmpty) {
        this.logger.lifecycle("No *.po files found for this source set '{}'.", sourceSetName)
        return@doFirst
      }
      project.fileTree(outDir).filter { it.isFile && it.name.endsWith(".mo") }.forEach { it.delete() }
      source.asFileTree.forEach {
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
            logger.warn("WARNING: msgfmt takes longer than 2 minutes to convert ${it.absolutePath} . Aborting now!")
          }
        } catch (e: IOException) {
          compilationFailed = true
          logger.warn("Failed to convert *.po file to *.mo file. Probably xgettext is not installed on your machine!")
        }
      }
      if (compilationFailed) {
        project.gradle.buildFinished { project.logger.error("WARNING: Not all i18n files have been built! Some *.po files could not be converted to *.mo files!\nWARNING: Maybe the program xgettext is not installed on your machine! It is required for processing *.po files.") }
      }
    }
  }
}

