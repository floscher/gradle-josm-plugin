package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Outputs a file containing the absolute paths of all Java source files
 */
open class GenerateFileList
  /**
   * @property outFile
   * The file to which the file list is written. Gets overwritten when this task executes.
   * @property srcSet
   * The source set for which the file list is generated.
   */
  @Inject
  constructor(@Internal val outFile: File, private val srcSet: SourceSet): DefaultTask() {

  init {
    project.afterEvaluate {
      outputs.file(outFile)
      inputs.files(srcSet.java.asFileTree.files)
    }
  }

  @TaskAction
  fun action() {
    outFile.delete()
    outFile.parentFile.mkdirs()
    outFile.writer().use { writer ->
      inputs.files.forEach {
        writer.write("${it.absolutePath}\n")
      }
    }
  }
}
