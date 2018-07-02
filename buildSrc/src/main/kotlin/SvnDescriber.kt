package org.openstreetmap.josm.gradle.plugin

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class SvnDescriber(val workTree: File): Describer {

  @Throws(IOException::class)
  override fun describe(dirty: Boolean): String {
    val process = ProcessBuilder("svn", "info").directory(workTree).start()
    if (process.waitFor(2, TimeUnit.MINUTES) && process.exitValue() == 0) {
      val prefix = "Revision: "
      val description = process.inputStream.bufferedReader().lines()
        .filter{ it.startsWith(prefix) }
        .map { it.substring(prefix.length) }
        .findFirst()
        .orElseThrow {
          IOException("`svn info` did not respond with a line starting with $prefix")
        }
      return if (dirty && isDirty()) {
        "r$description-dirty"
      } else {
        "r$description"
      }
    }
    throw IOException("Could not determine SVN revision of ${workTree.absolutePath}")
  }

  @Throws(IOException::class)
  fun isDirty(): Boolean {
    val process = ProcessBuilder("svn", "status", "-q").directory(workTree).start()
    if (process.waitFor(2, TimeUnit.MINUTES) && process.exitValue() == 0) {
      val linesCount = process.inputStream.bufferedReader().lines().count()
      return linesCount >= 1
    }
    throw IOException("Could not determine SVN dirty status of ${workTree.absolutePath}")
  }
}
