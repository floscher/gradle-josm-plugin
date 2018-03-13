package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.tasks.Delete
import org.openstreetmap.josm.gradle.plugin.getJosmExtension

open class CleanJosm : Delete() {
  init {
    group = "JOSM"
    project.afterEvaluate {
      delete(project.getJosmExtension().tmpJosmHome)
      description = "Delete JOSM configuration in `${project.getJosmExtension().tmpJosmHome}`"
    }
    doFirst {
      logger.lifecycle("Delete [{}]", targetFiles.files.map { it.absolutePath }.joinToString(", "))
    }
  }
}
