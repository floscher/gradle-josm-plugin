package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.openstreetmap.josm.gradle.plugin.config.I18nConfig
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File

/**
 * This task "compiles" several *.mo files to *.lang files.
 * For the language specified in [I18nConfig.mainLanguage], only the "msgid" is used (the text which will be translated).
 * For the other languages, the "msgstr" is used (the text which is already translated to this language).
 */
open class MoCompile : DefaultTask() {
  @Internal
  lateinit var poCompile: PoCompile

  /**
   * The [I18nSourceSet] for which the *.mo files will be compiled.
   */
  @Internal
  lateinit var sourceSet: I18nSourceSet

  init {
    project.afterEvaluate {
      val outDir = File(project.buildDir, "i18n/mo/" + sourceSet.name)

      inputs.files(poCompile)
      inputs.files(sourceSet.mo.asFileTree.files)
      outputs.dir(outDir)

      description = "Compile the *.mo gettext files of source set `${sourceSet.name}` to the *.lang format used by JOSM"

      doFirst {
        outDir.mkdirs()
        val inputFiles = inputs.files.asFileTree.files

        if (inputFiles.isEmpty()) {
          this.logger.lifecycle("No *.mo files found for this source set '{}'.", sourceSet.name)
        } else {
          logger.lifecycle("Reading the *.mo files…")
          project.fileTree(outDir).filter { it.isFile && it.name.endsWith(".lang") }.forEach { it.delete() }
          val langMap = mutableMapOf<String, Map<MsgId, MsgStr>>()
          inputFiles.forEach {
            logger.lifecycle("  ${it.absolutePath} …" + if (langMap.containsKey(it.nameWithoutExtension)) {
              " (will overwrite existing file!)"
            } else {
              ""
            })
            langMap[it.nameWithoutExtension] = MoReader(it.toURI().toURL()).readFile()
          }
          val projectDescription = project.extensions.josm.manifest.description
          if (projectDescription != null) {
            langMap.forEach { lang, map ->
              val translation = map.get(MsgId(MsgStr(projectDescription)))
              if (translation != null) {
                project.extensions.josm.manifest.translatedDescription(lang, translation.strings.first());
              }
            }
          }

          logger.lifecycle("Writing the *.lang files into ${outDir.absolutePath} …")
          LangWriter().writeLangFile(outDir, langMap, project.extensions.josm.i18n.mainLanguage)
        }
      }
    }
  }
}
