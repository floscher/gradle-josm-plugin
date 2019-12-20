package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.config.I18nConfig
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import javax.inject.Inject

/**
 * This task "compiles" several *.mo files to *.lang files.
 * For the language specified in [I18nConfig.mainLanguage], only the "msgid" is used (the text which will be translated).
 * For the other languages, the "msgstr" is used (the text which is already translated to this language).
 * @property poCompile The task for compiling *.po files to *.mo files. Its outputs are used as inputs for this task
 * @property sourceSet The [I18nSourceSet] for which the *.mo files will be compiled
 */
open class MoCompile @Inject constructor(
  @InputFiles val poCompile: PoCompile,
  @Internal val sourceSet: I18nSourceSet
): DefaultTask() {

  /**
   * The target directory where the *.lang files will be placed (automatically initialized after the project is evaluated)
   */
  @OutputDirectory
  lateinit var outDir: File

  @InputFiles
  val moSources = sourceSet.mo

  init {
    project.afterEvaluate {
      outDir = File(project.buildDir, "i18n/mo/" + sourceSet.name)
      description = "Compile the *.mo gettext files of source set `${sourceSet.name}` to the *.lang format used by JOSM"
    }
  }

  /**
   * The main task action, compiles the MO files to *.lang files and puts them into the [outDir].
   */
  @ExperimentalUnsignedTypes
  @TaskAction
  fun action() {
    outDir.mkdirs()
    val inputFiles = inputs.files.asFileTree.files

    if (inputFiles.isEmpty()) {
      this.logger.lifecycle("No *.mo files found for this source set '{}'.", sourceSet.name)
    } else {
      logger.lifecycle("Reading the *.mo files…")
      project.fileTree(outDir)
        .filter { it.isFile && it.name.endsWith(".lang") }
        .forEach { it.delete() }

      inputFiles.groupBy { it.parentFile.absolutePath }.forEach { dir, files ->
        logger.lifecycle("  from $dir : ${files.map { it.nameWithoutExtension }.sorted().joinToString(", ")}")
      }

      val langMap = mutableMapOf<String, Map<MsgId, MsgStr>>()
      inputFiles.forEach {
        langMap[it.nameWithoutExtension] = MoReader(it.toURI().toURL()).readFile()
      }

      logger.lifecycle("Writing the *.lang files into ${outDir.absolutePath} …")
      LangWriter().writeLangFile(outDir, langMap, project.extensions.josm.i18n.mainLanguage)

      inputFiles.groupBy { it.nameWithoutExtension }.filter { it.value.size >= 2 }.forEach { lang, paths ->
        val warnMsg = "\nWARNING: For language $lang there are multiple *.mo files, of which only the last one in the following list is used:\n  * ${paths.joinToString("\n  * ")}\n"
        logger.warn(warnMsg)
        project.gradle.buildFinished { logger.warn(warnMsg) }
      }
    }
  }
}
