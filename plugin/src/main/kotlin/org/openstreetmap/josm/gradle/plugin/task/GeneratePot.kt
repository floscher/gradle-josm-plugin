package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.config.I18nConfig
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.time.Year
import javax.inject.Inject

/**
 * Create *.pot file (gettext file format), which contains all translatable strings.
 * This file can then be handed to the translators to translate into other languages.
 *
 * For this task to work the command line tool `xgettext` is required!
 */
open class GeneratePot
  /**
   * @property fileListGenTask
   * The task that generates a list of all source files. That file is needed to tell xgettext, which files it should examine.
   */
  @Inject
  constructor(
    objectFactory: ObjectFactory,
    @get:InputFiles
    val inFiles: Provider<out Set<File>>
  ): Exec() {

  @get:Input
  val baseName: Property<String> = objectFactory.property(String::class.java).value(
    project.provider {
      "josm-plugin_" + project.extensions.josm.pluginName
    }
  )

  @get:Input
  val i18nConfig: Property<I18nConfig> = project.objects.property(I18nConfig::class.java).convention(
    project.provider{
      project.extensions.josm.i18n
    }
  )

  @get:Input
  val versionNumber: Property<String> = project.objects.property(String::class.java).convention(
    project.provider {
      project.version.toString()
    }
  )

  @get:Internal("Only a temporary file that is generated and renamed")
  val poFile: Provider<out RegularFile> = project.layout.buildDirectory.file(baseName.map { "i18n/pot/$it.po" })

  @get:OutputFile
  val potFile: Provider<out RegularFile> = project.layout.buildDirectory.file(baseName.map { "i18n/pot/$it.pot" })

  @get:OutputFile
  val srcFileListFile: Provider<out RegularFile> = project.layout.buildDirectory.file(baseName.map { "i18n/srcFileList/${it}.txt" })

  init {
    group = "JOSM-i18n"
    description = "Extracts translatable strings from the source code into a *.pot file. Requires the command line utility xgettext (part of GNU gettext)"

    executable = "xgettext"
    // static arguments
    args(
      "--from-code=UTF-8",
      "--language=Java",
      "--add-comments",
      "--sort-output",
      "-k", "-ktrc:1c,2", "-kmarktrc:1c,2", "-ktr", "-kmarktr", "-ktrn:1,2", "-ktrnc:1c,2,3"
    )
  }

  @Internal
  final override fun getWorkingDir() = project.projectDir
  final override fun setWorkingDir(dir: Any) = throw UnsupportedOperationException()

  @TaskAction
  final override fun exec() {
    // Finalize properties and make available as types that are easier to work with
    val baseName: String = baseName.apply { finalizeValue() }.get()

    // Make the providers accessible as `File` objects
    val poFile = poFile.get().asFile
    val potFile = potFile.get().asFile
    val srcFileListFile = srcFileListFile.get().asFile

    logger.lifecycle("Writing list of ${inFiles.get().size} files to ${srcFileListFile.absolutePath} …")
    srcFileListFile.writeText(inFiles.get().joinToString("\n", postfix = "\n") {
      it.relativeTo(workingDir).path // using relative path, so the checksum is reproducible
    })

    // dynamic arguments
    args(
      "--files-from=${srcFileListFile.absolutePath}",
      "--output-dir=${poFile.parentFile.absolutePath}",
      "--default-domain=$baseName",
      "--package-name=$baseName",
      "--package-version=${versionNumber.get()}"
    )

    i18nConfig.get().bugReportEmail?.let {
      args("--msgid-bugs-address=$it")
    }
    i18nConfig.get().copyrightHolder?.let {
      args("--copyright-holder=$it")
    }

    logger.lifecycle(commandLine.joinToString("\n  "))

    super.exec()

    try {
      moveFileAndReplaceStrings(
        poFile,
        potFile,
        { line ->
          if (line.startsWith("#: ")) {
            line.substring(0, 3) + i18nConfig.get().pathTransformer(line.substring(3))
          } else line
        },
        mutableMapOf(
          Pair("(C) YEAR", "(C) " + Year.now().value),
          Pair("charset=CHARSET", "charset=UTF-8")
        ),
        project.extensions.findByType(JosmPluginExtension::class.java)?.let {
          """
          |
          |#. Plugin description for $project.name
          |msgid "${it.manifest.description}"
          |msgstr ""
          |
          """.trimMargin()
        } ?: ""
      )
    } catch (e: IOException) {
      throw TaskExecutionException(this, e)
    }
  }

  final override fun args(vararg args: Any): Exec = super.args(*args)
  final override fun dependsOn(vararg paths: Any): Task = super.dependsOn(*paths)

  @Throws(IOException::class)
  private fun moveFileAndReplaceStrings(src: File, dest: File, lineTransform: (String) -> String, replacements: MutableMap<String,String>, appendix: String?) {
    val reader = if (src.exists()) src.inputStream() else ByteArrayInputStream(ByteArray(0))
    var content = reader.bufferedReader().lineSequence().map { lineTransform.invoke(it) }.joinToString("\n")
    for ((key, value) in replacements) {
      content = content.replaceFirst(key, value)
    }
    if (appendix != null) {
      content += '\n' + appendix
    }
    dest.writeText(content)
    src.delete()
  }
}
