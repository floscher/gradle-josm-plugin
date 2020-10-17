package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
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
        "  %s after %.2f seconds.",
        if (it.state.failure == null) "\uD83C\uDFC1 Finished" else "❌ Failed",
        Duration.between(it.extensions.extraProperties.get(startTimePropKey) as Instant, Instant.now()).toMillis() / 1e3
      ))
    }
  }
}

/**
 * Adds a log message to the end of each task of type [PublishToMavenRepository] that contains which artifact was published and to where
 */
fun TaskExecutionGraph.logPublishedMavenArtifacts() {
  afterTask {
    if (it is AbstractPublishToMaven) {
      val url = if (it is PublishToMavenRepository) {
        it.repository.url
      } else if (it is PublishToMavenLocal) {
        "Maven local (normally `~/.m2/repository` )"
      } else null

      if (url != null) {
        it.logger.lifecycle("""
        |  📦 Published artifact:
        |       to URL: ${url}
        |        Group: ${it.publication.groupId}
        |           ID: ${it.publication.artifactId}
        |      Version: ${it.publication.version}
        |
        """.trimMargin())
      }
    }
  }
}

/**
 * Enable logging of all skipped tasks after the build of the project finishes.
 */
fun Project.logSkippedTasks() {
  gradle.buildFinished {
    var wereTasksSkipped = false
    rootProject.allprojects.flatMap { it.tasks }.forEach {
      if (it.state.skipped) {
        if (!wereTasksSkipped) {
          it.logger.lifecycle("\nSkipped tasks:")
          wereTasksSkipped = true
        }
        it.logger.lifecycle(" ⏭️  ${it.path} (${it.state.skipMessage})")
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
  doFirst {
    reports {
      it.csv.isEnabled = true
    }
  }
  val logTask = project.task("${name}Log") {
    it.actions = listOf(
      Action {
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
          .map{ line -> line.split(',') }
          .forEach { lineCells ->
            require(lineCells.size == headerLine.size)
            for (i in colNames.indices) {
              colValues[i] += lineCells[colIndices[i]].toInt()
            }
          }

        logger.lifecycle("Instruction coverage${coverageLogMessage(it.project, colValues[0], colValues[1])}")
        logger.lifecycle("     Branch coverage${coverageLogMessage(it.project, colValues[2], colValues[3])}")
        logger.lifecycle("       Line coverage${coverageLogMessage(it.project, colValues[4], colValues[5])}")
      }
    )
  }
  finalizedBy(logTask)
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
