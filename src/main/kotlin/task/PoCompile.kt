package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.InputDirectory
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

  @InputDirectory
  lateinit var source: SourceDirectorySet
  private lateinit var sourceSetName: String

  private var compilationFailed: Boolean = false

  fun setup(source: I18nSourceSet) {
    this.outDir = File(project.buildDir, "i18n/po/" + source.name)
    this.source = source.po
    this.sourceSetName = source.name
    description = "Compile the *.po text files of source set ${source.name} to the binary *.mo files"
  }

  init {
    doFirst {
      outDir.mkdirs();

      if (source.isEmpty) {
        this.logger.lifecycle("No *.po files found for this source set '{}'.", source.name)
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
          process.waitFor(2, TimeUnit.MINUTES)
          this.logger.lifecycle(" > " + process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText())
        } catch (e: IOException) {
          compilationFailed = true
          logger.lifecycle("Failed to convert *.po file to *.mo file. Probably xgettext is not installed on your machine!")
        }
      }
      if (compilationFailed) {
        project.gradle.buildFinished { project.logger.error("WARNING: Not all i18n files have been built! Some *.po files could not be converted to *.mo files!\nWARNING: The program xgettext might not be installed on your machine, which is required for processing *.po files.") }
      }
    }
  }
}

