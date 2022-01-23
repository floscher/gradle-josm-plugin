package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Locale
import javax.inject.Inject

public open class JacocoReportLogTask @Inject constructor(@get:Input public val reportTask: JacocoReport): DefaultTask() {
  @TaskAction
  public fun action() {
    val allLines = reportTask.reports.csv.outputLocation.asFile.get().readLines()
    val headerLine = allLines[0].split(',')
    val colNames = arrayOf(
      "INSTRUCTION_COVERED", "INSTRUCTION_MISSED",
      "BRANCH_COVERED", "BRANCH_MISSED",
      "LINE_COVERED", "LINE_MISSED"
    )
    val colValues = IntArray(colNames.size) { 0 }
    val colIndices = IntArray(colNames.size) { headerLine.indexOf(colNames[it]) }

    allLines.subList(1, allLines.size)
      .map { line -> line.split(',') }
      .forEach { lineCells ->
        require(lineCells.size == headerLine.size)
        for (i in colNames.indices) {
          colValues[i] += lineCells[colIndices[i]].toInt()
        }
      }

    logger.lifecycle("Instruction coverage${coverageLogMessage(project, colValues[0], colValues[1])}")
    logger.lifecycle("     Branch coverage${coverageLogMessage(project, colValues[2], colValues[3])}")
    logger.lifecycle("       Line coverage${coverageLogMessage(project, colValues[4], colValues[5])}")
  }


  /**
   * @return the given numbers of covered and missed lines formatted as [String], e.g. "25.0000 % (2 of 8)"
   */
  private fun coverageLogMessage(project: Project, coveredCount: Int, missedCount: Int) =
    String.format(
      Locale.UK,
      "%s: %.4f %% (%d of %d)",
      if (project.path == ":") "" else " ${project.path}",
      100 * coveredCount.toDouble() / (coveredCount + missedCount),
      coveredCount,
      coveredCount + missedCount
    )
}
