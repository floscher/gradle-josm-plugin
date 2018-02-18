package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.SourceSet
import java.io.File

open class GenerateFileList: DefaultTask() {
  lateinit var outFile: File
  lateinit var srcSet: SourceSet

  val sourceFiles: MutableSet<File> = mutableSetOf()

  init {
    project.afterEvaluate {
      outputs.file(outFile)
      inputs.files(srcSet.java.srcDirs)
    }
    doFirst{
      outFile.delete()
      outFile.parentFile.mkdirs()
      outFile.writer().use { writer ->
        inputs.files.forEach {
          it.walk()
            .filter{ !it.isDirectory() && it.canRead() }
            .forEach {
              sourceFiles.add(it)
              writer.write(it.absolutePath + "\n")
            }
        }
      }
    }
  }
}
