package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.tasks.Copy
import org.openstreetmap.josm.gradle.plugin.getJosmExtension
import java.io.File

open class InitJosmPrefs: Copy() {
  private val PREF_FILE_NAME = "preferences.xml"
  init {
    description = "Puts a default preferences.xml file into the temporary JOSM home directory"
    include(PREF_FILE_NAME)

    project.afterEvaluate {
      from(project.getJosmExtension().josmConfigDir)
      into(project.getJosmExtension().tmpJosmHome)
      if (source.isEmpty) {
        logger.debug("No default JOSM preference file found in ${project.getJosmExtension().josmConfigDir}/preferences.xml.")
      }
    }

    doFirst {
      if (File(destinationDir, PREF_FILE_NAME).exists()) {
        logger.lifecycle("JOSM preferences not copied, file is already present.\nIf you want to replace it, run the task 'cleanJosm' additionally.")
      } else {
        logger.lifecycle("Copy [{}] to {}…", source.files.map { it.absolutePath }.joinToString(", "), destinationDir.absolutePath)
      }
    }
  }
}
