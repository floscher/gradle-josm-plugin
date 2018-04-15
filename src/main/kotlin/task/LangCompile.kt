package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.file.DuplicatesStrategy
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

  @Internal
  lateinit var sourceSet: I18nSourceSet

  @Internal
  lateinit var moCompile: MoCompile

  init {
    project.afterEvaluate {
      val outDir = File(project.buildDir, "i18n/lang/${sourceSet.name}")

      from(moCompile)
      from(sourceSet.lang)
      into(outDir)

      includeEmptyDirs = false
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
      eachFile {
        /* Flatten directory tree, the other compile tasks do the same. Put everything in `data/`,
           because JOSM expects the files there. */
        it.path = "data/${it.sourceName}"
      }

      doFirst {
        logger.lifecycle("Copy *.lang files to $outDir/data …")
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
