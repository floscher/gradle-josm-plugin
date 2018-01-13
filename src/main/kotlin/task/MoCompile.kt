package task

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.openstreetmap.josm.gradle.plugin.getJosmExtension
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.task.PoCompile
import java.io.File

/**
 * This task "compiles" several *.mo files to *.lang files.
 * For the language specified in [I18nConfig.mainLanguage], only the "msgid" is used (the text which will be translated).
 * For the other languages, the "msgstr" is used (the text which is already translated to this language).
 */
open class MoCompile : DefaultTask() {
  @OutputDirectory
  lateinit var outDir: File

  private lateinit var source: SourceDirectorySet
  private lateinit var sourceSetName: String

  private lateinit var poCompileTask: Task

  fun setup(source: I18nSourceSet, poCompileTask: PoCompile) {
    this.outDir = File(project.buildDir, "i18n/mo/" + source.name)
    this.source = source.mo
    inputs.files(source.mo.files)
    this.sourceSetName = source.name
    inputs.files(poCompileTask)
    this.poCompileTask = poCompileTask

    description = "Compile the *.mo gettext files of source set $sourceSetName to the *.lang format used by JOSM"
  }

  init {
    doFirst {
      logger.lifecycle("Compiling the *.lang files for ${outDir.absolutePath}…")
      outDir.mkdirs()

      val files = source.asFileTree.files.plus(poCompileTask.outputs.files.asFileTree.files).filter{ it.isFile }
      if (files.isEmpty()) {
        this.logger.lifecycle("No *.mo files found for this source set '{}'.", sourceSetName)
      }
      project.fileTree(outDir).filter { it.isFile && it.name.endsWith(".lang") }.forEach { it.delete() }
      val langMap = mutableMapOf<String, Map<MsgId, MsgStr>>()
      files.forEach {
        logger.lifecycle("Compiling ${it.absolutePath}…")
        langMap[it.nameWithoutExtension] = MoReader(it.toURI().toURL()).readFile()
      }
      logger.lifecycle("Write *.lang files…")
      LangWriter().writeLangFile(File(outDir, "data"), langMap, project.getJosmExtension().i18n.mainLanguage)
    }
  }
}
