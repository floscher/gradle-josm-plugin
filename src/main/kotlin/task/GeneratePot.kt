package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.josm
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

open class GeneratePot: Exec() {
  private lateinit var outBaseName: String
  @Internal
  lateinit var fileListGenTask: GenerateFileList

  init {
    group = "JOSM-i18n"
    description = "Extracts translatable strings from the source code into a *.pot file. Requires the command line utility xgettext (part of GNU gettext)"

    val outDir = File(project.buildDir, "i18n")
    outputs.dir(outDir)

    workingDir = project.getProjectDir()
    executable = "xgettext"
    args(
      "--from-code=UTF-8", "--language=Java",
      "--output-dir=" + outDir.absolutePath,
      "--add-comments",
      "--sort-output",
      "-k", "-ktrc:1c,2", "-kmarktrc:1c,2", "-ktr", "-kmarktr", "-ktrn:1,2", "-ktrnc:1c,2,3"
    )

    project.afterEvaluate {
      dependsOn(fileListGenTask)
      inputs.files(fileListGenTask.inputs.files)

      outBaseName = "josm-plugin_" + it.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName
      args(
        "--files-from=${fileListGenTask.outFile.absolutePath}",
        "--default-domain=$outBaseName",
        "--package-name=$outBaseName",
        "--package-version=${it.version}"
      )
      if (it.extensions.josm.i18n.bugReportEmail != null) {
        args("--msgid-bugs-address=" + it.extensions.josm.i18n.bugReportEmail)
      }
      if (it.extensions.josm.i18n.copyrightHolder != null) {
        args("--copyright-holder=" + it.extensions.josm.i18n.copyrightHolder)
      }

      doFirst {
        if (!outDir.exists()) {
          outDir.mkdirs()
        }
        logger.lifecycle(commandLine.joinToString("\n  "))
      }
    }


    doLast {
      val destFile = File(outDir, outBaseName + ".pot")
      val replacements = mutableMapOf<String, String>()
      replacements.put("(C) YEAR", "(C) " + Year.now().getValue())
      replacements.put("charset=CHARSET", "charset=UTF-8")
      try {
        moveFileAndReplaceStrings(
          File (outDir, "$outBaseName.po"),
          destFile,
          { line ->
            if (line.startsWith("#: ")) {
              line.substring(0, 3) + project.extensions.josm.i18n.pathTransformer.invoke(line.substring(3))
            } else line
          },
          replacements,
          "\n#. Plugin description for " + project.getName() + "\nmsgid \"" + project.extensions.josm.manifest.description + "\"\nmsgstr \"\"\n"
        )
      } catch (e: IOException) {
        throw TaskExecutionException(this, e)
      }
    }
  }

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
