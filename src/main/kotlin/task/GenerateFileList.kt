package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * Outputs a file containing the absolute paths of all Java source files
 */
open class GenerateFileList: DefaultTask() {
  /**
   * The file to which the file list is written. Gets overwritten when this task executes.
   */
  @Internal
  lateinit var outFile: File

  /**
   * The source set for which the file list is generated.
   */
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
