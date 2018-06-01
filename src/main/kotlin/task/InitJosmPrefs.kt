package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.tasks.Copy
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File

/**
 * A simple copy task that copies a `preferences.xml` file from [JosmPluginExtension.josmConfigDir] to
 * [JosmPluginExtension.tmpJosmPrefDir] if there is not already one in the destination.
 *
 * This way you can provide a default JOSM configuration for the [RunJosmTask].
 */
open class InitJosmPrefs: Copy() {
  private val PREF_FILE_NAME = "preferences.xml"
  init {
    description = "Puts a default preferences.xml file into the temporary JOSM home directory"
    include(PREF_FILE_NAME)

    project.afterEvaluate {
      from(project.extensions.josm.josmConfigDir)
      into(project.extensions.josm.tmpJosmPrefDir)
      if (source.isEmpty) {
        logger.debug("No default JOSM preference file found in ${project.extensions.josm.josmConfigDir}/preferences.xml.")
      }
      doFirst {
        if (File(destinationDir, PREF_FILE_NAME).exists()) {
          exclude("*")
          logger.lifecycle("Default JOSM preferences not copied, file is already present. If you want to replace it, run the task 'cleanJosm' additionally.")
        } else {
          logger.lifecycle("Copy [{}] to {}…", source.files.map { it.absolutePath }.joinToString(", "), destinationDir.absolutePath)
        }
      }
    }


  }
}
