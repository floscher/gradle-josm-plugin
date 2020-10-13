package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import java.io.FileNotFoundException

/**
 * Task for downloading the current state of the translations from transifex.com
 */
open class TransifexDownload : Exec() {
  init {
    group = "JOSM-i18n"
    description = "Download the current state of translations from transifex.com . Requires the Transifex client to be installed."
    commandLine = listOf("tx", "pull", "-a", "--mode", "onlytranslated")
    workingDir = project.projectDir
    project.afterEvaluate {
      doFirst {
        if (!File(project.projectDir, ".tx/config").exists()) {
          throw TaskExecutionException(this, FileNotFoundException("You need a file `${project.projectDir}/.tx/config` for the Transifex client to work!"))
        }
        println(commandLine.joinToString("\n  ") + "\n===")
      }
      finalizedBy(project.tasks.withType(ShortenPoFiles::class.java))
    }
  }
}
