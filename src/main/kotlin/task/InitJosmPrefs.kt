package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File

/**
 * A simple copy task that copies a `preferences.xml` file from [JosmPluginExtension.josmConfigDir] to
 * [JosmPluginExtension.tmpJosmPrefDir] if there is not already one in the destination.
 *
 * This way you can provide a default JOSM configuration for the [RunJosmTask].
 */
open class InitJosmPrefs: DefaultTask() {
  companion object {
    private const val PREF_FILE_NAME = "preferences.xml"
  }

  init {
    project.afterEvaluate {
      val srcFile = File(project.extensions.josm.josmConfigDir, PREF_FILE_NAME)
      description = String.format(
        "Puts a custom preferences.xml file (%s%s) into the temporary JOSM home directory (%s).",
        srcFile.absolutePath,
        if (srcFile.exists()) { "" } else { ", does not yet exist" },
        project.extensions.josm.tmpJosmPrefDir.absolutePath
      )
    }
  }

  @TaskAction
  fun action() {
    val fromDir = project.extensions.josm.josmConfigDir
    val intoDir = project.extensions.josm.tmpJosmPrefDir
    if (!File(fromDir, PREF_FILE_NAME).exists()) {
      logger.lifecycle("No default JOSM preference file found in $fromDir/$PREF_FILE_NAME . JOSM will create a new one when started.")
    } else if (File(intoDir, PREF_FILE_NAME).exists()) {
      logger.lifecycle("Default JOSM preferences not copied, file is already present. If you want to replace it, run the task 'cleanJosm' additionally.")
    } else {
      project.copy {
        it.from(fromDir)
        it.into(intoDir)
        it.include(PREF_FILE_NAME)
      }
      logger.lifecycle("Copy {} to {}…", File(fromDir, PREF_FILE_NAME).absolutePath, intoDir.absolutePath)
    }
  }
}
