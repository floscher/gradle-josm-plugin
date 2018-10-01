package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Add logging of the task duration right after each task finishes.
 */
fun TaskExecutionGraph.logTaskDuration() {
  val startTimePropKey = "taskStartTime"
  beforeTask {
    it.extensions.extraProperties.set(startTimePropKey, Instant.now())
  }
  afterTask {
    if (!it.state.skipped) {
      it.logger.lifecycle(String.format(
        "  🏁 Finished after %.2f seconds.",
        Duration.between(it.extensions.extraProperties.get(startTimePropKey) as Instant, Instant.now()).toMillis() / 1e3
      ))
    }
  }
}

/**
 * Enable logging of all skipped tasks after the build of the project finishes.
 */
fun Project.logSkippedTasks() {
  gradle.buildFinished {
    var wereTasksSkipped = false
    tasks.forEach {
      if (it.state.skipped) {
        if (!wereTasksSkipped) {
          it.logger.lifecycle("\nSkipped tasks:")
          wereTasksSkipped = true
        }
        it.logger.lifecycle(" ⏭️  :${it.name} (${it.state.skipMessage})")
      }
    }
  }
}

/**
 * Enable logging of coverage for instructions, branches and lines to this task.
 * As a side effect, csv reporting is enabled for this task.
 * For better machine-readability, the coverage numbers are always printed with
 * decimal point (never decimal comma) and four decimal places.
 */
fun JacocoReport.logCoverage() {
  reports {
    it.csv.isEnabled = true
  }
  doLast {
    val allLines = reports.csv.destination.readLines()
    val headerLine = allLines[0].split(',')
    val colNames = arrayOf(
      "INSTRUCTION_COVERED", "INSTRUCTION_MISSED",
      "BRANCH_COVERED", "BRANCH_MISSED",
      "LINE_COVERED", "LINE_MISSED"
    )
    val colValues = IntArray(colNames.size) { 0 }
    val colIndices = IntArray(colNames.size) { headerLine.indexOf(colNames[it]) }
    allLines.subList(1, allLines.size)
      .map{ it.split(',') }
      .forEach { lineCells ->
        require(lineCells.size == headerLine.size)
        for (i in 0 until colNames.size) {
          colValues[i] += lineCells[colIndices[i]].toInt()
        }
      }

    logger.lifecycle("Instruction coverage: ${coverageLogMessage(colValues[0], colValues[1])}")
    logger.lifecycle("     Branch coverage: ${coverageLogMessage(colValues[2], colValues[3])}")
    logger.lifecycle("       Line coverage: ${coverageLogMessage(colValues[4], colValues[5])}")
  }
}

/**
 * @return the given numbers of covered and missed lines formatted as [String], e.g. "25.0000 % (2 of 8)"
 */
private fun coverageLogMessage(coveredCount: Int, missedCount: Int) =
  String.format(
    Locale.UK,
    "%.4f %% (%d of %d)",
    100 * coveredCount.toDouble() / (coveredCount + missedCount),
    coveredCount,
    coveredCount + missedCount
  )
