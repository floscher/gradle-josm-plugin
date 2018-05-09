package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import java.io.File

/**
 * This is not really a compilation task, it's only named like that analogous to [MoCompile] and [PoCompile].
 *
 * It copies (more precisely it [Sync]s) the *.lang files to `$buildDir/i18n/lang/$sourceSetName/data`
 */
open class LangCompile : Sync() {

  /**
   * The source set from which all *.lang files are synced to the destination
   */
  @Internal
  lateinit var sourceSet: I18nSourceSet

  /**
   * The task for compiling *.mo files to *.lang files. These outputs are then used as inputs for this task.
   */
  @Internal
  lateinit var moCompile: MoCompile

  @Input
  val subdirectory = "data"

  init {
    project.afterEvaluate {
      destinationDir = File(project.buildDir, "i18n/lang/")

      from(moCompile)
      from(sourceSet.lang)

      includeEmptyDirs = false
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      eachFile {
        /* Flatten directory tree, the other compile tasks do the same. Put everything in `data/`,
           because JOSM expects the files there. */
        it.path = "$subdirectory/${it.sourceName}"
      }

      doFirst {
        logger.lifecycle("Copy *.lang files to ${destinationDir.absolutePath}/$subdirectory …")
        val langs = HashSet<String>()
        source.files.forEach {
          logger.lifecycle(
            "  ${it.path} …" +
            if (langs.add(it.nameWithoutExtension)) {
              ""
            } else {
              " (will overwrite existing file!)"
            }
          )
        }
      }
    }
  }
}
