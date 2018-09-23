package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import java.time.Duration
import java.time.Instant

fun TaskExecutionGraph.logTaskDuration() {
  val startTimePropKey = "taskStartTime"
  beforeTask {
    it.extensions.extraProperties.set(startTimePropKey, Instant.now())
  }
  afterTask {
    it.logger.lifecycle(String.format(
      "  🏁 Finished after %.2f seconds.",
      Duration.between(it.extensions.extraProperties.get(startTimePropKey) as Instant, Instant.now()).toMillis() / 1e3
    ))
  }
}

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
