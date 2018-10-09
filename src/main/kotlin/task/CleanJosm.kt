package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File
import java.util.Locale

/**
 * Virtual task that coordinates deletion of the temporary JOSM preferences.
 *
 * This task creates three subtasks with the same name as itself, only with the suffixes `Cache`, `Pref` and `Userdata`.
 * Each one of these subtasks deletes one of the directories defined at [JosmPluginExtension.tmpJosmCacheDir],
 * [JosmPluginExtension.tmpJosmPrefDir] and [JosmPluginExtension.tmpJosmUserdataDir].
 *
 * This task depends on all three of these, so all subtasks run when you execute this task. But you can also run the
 * subtasks individually by calling them directly.
 */
open class CleanJosm : DefaultTask() {
  init {
    group = "JOSM"
    project.afterEvaluate {
      addDependentTask("Cache", project.extensions.josm.tmpJosmCacheDir)
      addDependentTask("Pref", project.extensions.josm.tmpJosmPrefDir)
      addDependentTask("Userdata", project.extensions.josm.tmpJosmUserdataDir)
      description = "Delete temporary JOSM directories used for the `runJosm` and `debugJosm` tasks (for preferences, cache and userdata). Run `cleanJosmCache`, `cleanJosmPref` or `cleanJosmUserdata` to delete only one of them."
    }
  }
  private fun addDependentTask(taskSuffix: String, dir: File) {
    val task = project.tasks.create("$name$taskSuffix", Delete::class.java)
    task.description = "Delete ${taskSuffix.toLowerCase(Locale.UK)} directory (${dir.absolutePath})"
    task.delete(dir)
    task.doFirst {
      logger.lifecycle("Delete [{}]", task.targetFiles.files.joinToString(", ") { it.absolutePath })
    }
    dependsOn(task)
  }
}
