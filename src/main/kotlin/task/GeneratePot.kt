package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Task
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
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
  constructor(private val fileListGenTask: GenerateFileList): Exec() {

  private lateinit var outBaseName: String

  @InputFiles
  lateinit var inFiles: Set<File>

  @OutputDirectory
  val outDir = File(project.buildDir, "i18n/pot")

  init {
    group = "JOSM-i18n"
    description = "Extracts translatable strings from the source code into a *.pot file. Requires the command line utility xgettext (part of GNU gettext)"

    dependsOn(fileListGenTask)

    project.gradle.projectsEvaluated {
      inFiles = fileListGenTask.inFiles
    }

    executable = "xgettext"
    args(
      "--from-code=UTF-8", "--language=Java",
      "--output-dir=" + outDir.absolutePath,
      "--add-comments",
      "--sort-output",
      "-k", "-ktrc:1c,2", "-kmarktrc:1c,2", "-ktr", "-kmarktr", "-ktrn:1,2", "-ktrnc:1c,2,3"
    )
  }

  @TaskAction
  final override fun exec() {
    workingDir = project.projectDir

    outBaseName = "josm-plugin_" + project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName
    args(
      "--files-from=${fileListGenTask.outFile.absolutePath}",
      "--default-domain=$outBaseName",
      "--package-name=$outBaseName",
      "--package-version=${project.version}"
    )

    project.extensions.josm.i18n.bugReportEmail?.let {
      args("--msgid-bugs-address=$it")
    }
    project.extensions.josm.i18n.copyrightHolder?.let {
      args("--copyright-holder=$it")
    }

    if (!outDir.exists()) {
      outDir.mkdirs()
    }
    logger.lifecycle(commandLine.joinToString("\n  "))

    super.exec()

    try {
      moveFileAndReplaceStrings(
        File(outDir, "$outBaseName.po"),
        File(outDir, "$outBaseName.pot"),
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
    val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(dest), StandardCharsets.UTF_8))
    val reader = BufferedReader(InputStreamReader(FileInputStream(src), StandardCharsets.UTF_8))
    reader.useLines { lines ->
      writer.use { out ->
        for (line in lines) {
          var varLine = line
          // Apply the line transformer
          varLine = lineTransform.invoke(varLine)
          // Replace all replacements
          replacements.filter { varLine.contains(it.key) }
            .forEach { key, value ->
              varLine = varLine.replace(key, value)
              replacements.remove(key)
            }
          out.write(varLine + "\n")
        }
        if (appendix != null) {
          out.write(appendix)
        }
      }
    }
    src.delete()
  }
}
