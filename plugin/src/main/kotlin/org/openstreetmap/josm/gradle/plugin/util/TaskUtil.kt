package org.openstreetmap.josm.gradle.plugin.util

import org.gradle.api.Action
import org.gradle.api.Task

/**
 * Workaround for
 * [Gradle validation problem "Implementation unknown"](https://docs.gradle.org/7.3.3/userguide/validation_problems.html#implementation_unknown)
 */
public fun <T: Task> T.doFirst(config: (T) -> Unit): Task {
  doFirst(TaskAction {
    config(this)
  })
  return this
}

private class TaskAction(private val config: (Task) -> Unit): Action<Task> {
  override fun execute(t: Task): Unit = config(t)
}
