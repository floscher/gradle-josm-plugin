package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.openstreetmap.josm.gradle.plugin.task.JacocoReportLogTask
import java.time.Duration
import java.time.Instant

/**
 * Add logging of the task duration right after each task finishes.
 */
public fun TaskExecutionGraph.logTaskDuration() {
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
public fun TaskExecutionGraph.logPublishedMavenArtifacts() {
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
public fun Project.logSkippedTasks() {
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
public fun JacocoReport.logCoverage() {
  doFirst {
    reports {
      it.csv.required.value(true).finalizeValue()
    }
  }
  val logTask = project.tasks.register("${name}Log", JacocoReportLogTask::class.java, this)
  finalizedBy(logTask)
}
