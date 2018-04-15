package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import java.io.File

open class GenerateFileList: DefaultTask() {
  @Internal
  lateinit var outFile: File

  @Internal
  lateinit var srcSet: SourceSet

  init {
    project.afterEvaluate {
      outputs.file(outFile)
      inputs.files(srcSet.java.asFileTree.files)
    }
    doFirst{
      outFile.delete()
      outFile.parentFile.mkdirs()
      outFile.writer().use { writer ->
        inputs.files.forEach {
          writer.write("${it.absolutePath}\n")
        }
      }
    }
  }
}
