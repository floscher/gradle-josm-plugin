package org.openstreetmap.josm.gradle.plugin

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Use the command `svn info` to determine the SVN revision.
 * @param [workTree] the root directory of the SVN repository that you want to describe
 */
class SvnDescriber(val workTree: File): Describer {

  /**
   * @param [markSnapshots] if this is true, the revision number is appended with [Describer.SNAPSHOT_SUFFIX],
   *   when there are uncommitted changes in the repository.
   * @param [trimLeading] by default strips the leading `r` of the version number, set to `false` to keep it
   * @return the SVN revision in the format "r123"
   * @throws [IOException] if the process of `svn info` is not executed successfully within 2 minutes,
   *   or if the result does not contain a revision.
   */
  @Throws(IOException::class)
  override fun describe(markSnapshots: Boolean, trimLeading: Boolean): String {
    val process = ProcessBuilder("svn", "info").directory(workTree).start()
    if (process.waitFor(2, TimeUnit.MINUTES) && process.exitValue() == 0) {
      val prefix = "Revision: "
      val description = process.inputStream.bufferedReader().lines()
        .filter{ it.startsWith(prefix) }
        .map { it.substring(prefix.length) }
        .findFirst()
        .orElseThrow {
          IOException("`svn info` did not respond with a line starting with `$prefix`")
        }
      return "${if (trimLeading) "" else "r"}$description${if (markSnapshots && isDirty()) Describer.SNAPSHOT_SUFFIX else ""}"
    }
    throw IOException("Could not determine SVN revision of ${workTree.absolutePath}")
  }

  /**
   * @return `true` if there are uncommitted changes in the repository, `false` otherwise.
   * @throws [IOException] if `svn status` does not execute successfully within 2 minutes
   */
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
