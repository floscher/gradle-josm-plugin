package org.openstreetmap.josm.gradle.plugin.task

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

open class MarkdownToHtml: SourceTask() {

  var destDir: File? = null

  init {
    project.afterEvaluate {
      outputs.dir(requireNotNull(destDir))
    }
  }

  @TaskAction
  fun convert() {
    val destDir = requireNotNull(destDir)

    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()

    val files = getSource().files.filter { it.extension.toLowerCase(Locale.ENGLISH) == "md" }

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

