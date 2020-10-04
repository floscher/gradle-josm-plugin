package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.util.josm
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
    val subtasks = setOf(
      addDependentTask("Cache", project.provider { project.extensions.josm.tmpJosmCacheDir }),
      addDependentTask("Pref", project.provider { project.extensions.josm.tmpJosmPrefDir }),
      addDependentTask("Userdata", project.provider { project.extensions.josm.tmpJosmUserdataDir })
    )
    super.setDependsOn(subtasks)
    description = "Delete temporary JOSM directories used for the `runJosm` and `debugJosm` tasks (for preferences, cache and userdata). Run `${subtasks.joinToString("` or `") { it.name } }` to delete only one of them."
  }
  private fun addDependentTask(taskSuffix: String, dir: Provider<out File>) = project.tasks.register("$name$taskSuffix", Delete::class.java) { task ->
    task.description = "Delete ${taskSuffix.toLowerCase(Locale.UK)} directory used when running JOSM"
    task.delete(dir)
    task.doFirst {
      logger.lifecycle("Delete ${task.targetFiles.asFileTree.files.size} files in ${dir.get().absolutePath}")
    }
  }
}
