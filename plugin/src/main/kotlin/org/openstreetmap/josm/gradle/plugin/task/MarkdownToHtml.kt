package org.openstreetmap.josm.gradle.plugin.task

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Convert all `*.md` files found among the source files of this task to *.html files.
 *
 * By default this is not used in the `gradle-josm-plugin`, but tasks of this type can be created as needed by projects using the `gradle-josm-plugin`.
 */
open class MarkdownToHtml: SourceTask() {

  /**
   * The directory where the *.html files will be created in.
   * All files will be created directly inside the directory (not in subdirectories)
   * using the name of the source file, replacing the ".md" extension with ".html".
   */
  @OutputDirectory
  lateinit var destDir: File

  @TaskAction
  fun convert() {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()

    val files = source.files.filter { it.extension.lowercase() == "md" }

    if (files.map { it.nameWithoutExtension }.distinct().size < files.size) {
      throw TaskExecutionException(this, GradleException("There are multiple files with the same name for task ${this.name}!"))
    }

    files.forEach { file ->
      val destFile = File(destDir, file.nameWithoutExtension + ".html")
      logger.lifecycle("Convert ${file.absolutePath}\n      → ${destFile.absolutePath}\n")
      destFile.parentFile.mkdirs()
      destFile.writer(Charsets.UTF_8).use {
        it.write("<!DOCTYPE html><meta charset=\"utf-8\"><title>${file.name}</title>")
        renderer.render(parser.parseReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)), it)
      }
    }
  }
}

