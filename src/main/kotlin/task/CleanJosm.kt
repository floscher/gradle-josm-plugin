package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File
import java.util.Locale

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
      logger.lifecycle("Delete [{}]", task.targetFiles.files.map { it.absolutePath }.joinToString(", "))
    }
    dependsOn(task)
  }
}
