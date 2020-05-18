package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
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

  @get:Internal("The files inside the directory are individually added as output files")
  val outDir: DirectoryProperty = objectFactory.directoryProperty().value(
    project.layout.buildDirectory.dir("i18n/pot")
  )

  @get:Internal("Only a temporary file that is generated and renamed")
  val poFile: Provider<out RegularFile> = outDir.file(baseName.map { "$it.po" })

  @get:OutputFile
  val potFile: Provider<out RegularFile> = outDir.file(baseName.map { "$it.pot" })

  @get:OutputFile
  val srcFileListFile: Provider<out RegularFile> = outDir.file(baseName.map { "${it}_srcFileList.txt" })

  init {
    group = "JOSM-i18n"
    description = "Extracts translatable strings from the source code into a *.pot file. Requires the command line utility xgettext (part of GNU gettext)"

    workingDir = project.projectDir

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

  @TaskAction
  final override fun exec() {
    // Finalize properties and make available as types that are easier to work with
    val baseName: String = baseName.apply { finalizeValue() }.get()
    val outDir: File = outDir.apply { finalizeValue() }.get().asFile

    // Make the providers accessible as `File` objects
    val poFile = poFile.get().asFile
    val potFile = potFile.get().asFile
    val srcFileListFile = srcFileListFile.get().asFile

    logger.lifecycle("Writing list of ${inFiles.get().size} files to ${srcFileListFile.absolutePath} …")
    srcFileListFile.writeText(inFiles.get().joinToString("\n", postfix = "\n") { it.absolutePath })

    // dynamic arguments
    args(
      "--files-from=${srcFileListFile.absolutePath}",
      "--output-dir=${outDir.absolutePath}",
      "--default-domain=$baseName",
      "--package-name=$baseName",
      "--package-version=${project.version}"
    )

    project.extensions.josm.i18n.bugReportEmail?.let {
      args("--msgid-bugs-address=$it")
    }
    project.extensions.josm.i18n.copyrightHolder?.let {
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
            line.substring(0, 3) + project.extensions.josm.i18n.pathTransformer.invoke(line.substring(3))
          } else line
        },
        mutableMapOf(
          Pair("(C) YEAR", "(C) " + Year.now().value),
          Pair("charset=CHARSET", "charset=UTF-8")
        ),
        '\n' + """
        |#. Plugin description for $project.name
        |msgid "${project.extensions.josm.manifest.description}"
        |msgstr ""
        """.trimMargin() + '\n'
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
