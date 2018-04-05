package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
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
  @OutputDirectory
  lateinit var outDir: File

  @Internal
  private lateinit var sourceSetName: String
  @Internal
  private lateinit var sourceFiles: Set<File>

  fun setup(sourceSet: I18nSourceSet, poCompileTask: PoCompile) {
    this.outDir = File(project.buildDir, "i18n/mo/" + sourceSet.name)

    this.sourceFiles = sourceSet.mo.asFileTree.files.plus(poCompileTask.outputs.files.asFileTree.files).filter { it.isFile }.toSet()
    inputs.files(sourceFiles)
    this.sourceSetName = sourceSet.name
    inputs.files(poCompileTask)

    description = "Compile the *.mo gettext files of source set $sourceSetName to the *.lang format used by JOSM"
  }

  init {
    doFirst {
      outDir.mkdirs()

      if (sourceFiles.isEmpty()) {
        this.logger.lifecycle("No *.mo files found for this source set '{}'.", sourceSetName)
        return@doFirst
      }
      logger.lifecycle("Compiling the *.lang files for ${outDir.absolutePath}…")
      project.fileTree(outDir).filter { it.isFile && it.name.endsWith(".lang") }.forEach { it.delete() }
      val langMap = mutableMapOf<String, Map<MsgId, MsgStr>>()
      sourceFiles.forEach {
        logger.lifecycle("Reading ${it.absolutePath}…")
        langMap[it.nameWithoutExtension] = MoReader(it.toURI().toURL()).readFile()
      }
      val projectDescription = project.extensions.josm.manifest.description
      if (projectDescription != null) {
        langMap.forEach {lang, map ->
          val translation = map.get(MsgId(MsgStr(projectDescription)))
          if (translation != null) {
            project.extensions.josm.manifest.translatedDescription(lang, translation.strings.first());
          }
        }
      }
      logger.lifecycle("Write *.lang files…")
      LangWriter().writeLangFile(File(outDir, "data"), langMap, project.extensions.josm.i18n.mainLanguage)
    }
  }
}
