package org.openstreetmap.josm.gradle.plugin.langconv

import org.openstreetmap.josm.gradle.plugin.i18n.io.LangReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

@ExperimentalUnsignedTypes
@Throws(IllegalArgumentException::class)
fun main(vararg args: String) {
  if (args.isEmpty()) {
    printUsage()
    System.exit(0)
  }

  val mode: LangconvMode = LangconvMode.values().firstOrNull {
    args.isNotEmpty() && it.name.equals(args.first(), true)
  } ?: failWithException(IllegalArgumentException("Unknown conversion mode '${args.firstOrNull() ?: ""}'! Only ${LangconvMode.values().joinToString(" or ") { it.name.toLowerCase(Locale.ENGLISH) }} are allowed!"))

  val inputArg = args.getOrNull(1) ?: failWithException(IllegalArgumentException("No input file or directory is given!"))
  val outputArg = args.getOrNull(2)

  val inputFile: File? = File(inputArg)
    .takeIf { it.exists() }
  val inputFiles: List<File> = inputFile
    ?.let {
      if (it.isDirectory) {
        it.listFiles(mode.inputFilter).toList()
      } else {
        listOf(it)
      }
    } ?: failWithException(IllegalArgumentException("The given input file/directory does not exist or you can't read from it!: ${ File(inputArg).safeCanonicalPath() }"))

  val filesWithSameName = inputFiles.groupBy { it.nameWithoutExtension }.values.filter { it.size > 1 }
  if (filesWithSameName.isNotEmpty()) {
    failWithException(IllegalArgumentException("The given input directory contains more than one file with the same name (${filesWithSameName.flatMap { it.map { it.name } }.joinToString()})! This would lead to name collisions with the output files."))
  }

  val outputDir: File =
    outputArg
      ?.let { File(outputArg) }
      ?: inputFile.let {
        if (it.isDirectory) it else it.parentFile
      }

  if (!(outputDir.exists() || outputDir.mkdirs())) {
    failWithException(IOException("Can't create the output directory!: ${outputDir.safeCanonicalPath()}"))
  } else if (!outputDir.isDirectory) {
    failWithException(java.lang.IllegalArgumentException("The argument given as output directory is not a directory!: ${outputDir.safeCanonicalPath()}"))
  }

  val strings = mode.readFunction.invoke(inputFiles)
  val baseLang = strings["en"] ?: if (mode.isNeedsBaseLanguage) failWithException(IllegalArgumentException("No strings in base language 'en' found! Note, that at the moment the base language can't be changed for the 'langconv' program.")) else strings.flatMap { it.value.keys }.map { it to it.id }.toMap()
  val numBaseStrings = baseLang.filter { it.key != MoWriter.EMPTY_MSGID }.size

  println("Base language is 'en' with $numBaseStrings strings\n")

  val maxKeyLength = strings.keys.map { it.length }.max() ?: 0
  val maxStringNumberLength = strings.values.map { it.size.toString().length }.max() ?: 0
  println(
    strings.entries
      .filter { it.key != "en" }
      .sortedBy { it.key }
      .joinToString("\n") { stringEntry ->
        val numTranslated = stringEntry.value.keys.filter { it != MoWriter.EMPTY_MSGID }.size
        val percentage = numTranslated / numBaseStrings.toDouble() * 100
        val endChar = if (numTranslated == numBaseStrings) '▒' else '░'

        "${stringEntry.key.padStart(maxKeyLength + 2)}: ${numTranslated.toString().padStart(maxStringNumberLength)} strings (${String.format("%.2f", percentage).padStart(6)}% translated) $endChar${progressBarString(percentage).padEnd(25)}$endChar"
      }
  )

  mode.writeFunction.invoke(strings, outputDir)
  println("\n The files have been written successfully into ${outputDir.safeCanonicalPath()}")
}

private fun File.safeCanonicalPath(): String = try {
  this.canonicalPath
} catch (e: Exception) {
  when(e) {
    is IOException, is SecurityException -> this.absolutePath
    else -> throw e
  }
}


@ExperimentalUnsignedTypes
private fun failWithException(throwException: Exception): Nothing {
  println("\nERROR: ${throwException.localizedMessage}\n")
  printUsage()
  println()
  throw throwException
}

@ExperimentalUnsignedTypes
private fun printUsage() {
  println("""
    === Usage: ===

      java -jar langconv.jar mo2lang ‹input›
      java -jar langconv.jar mo2lang ‹input› ‹output›
      java -jar langconv.jar lang2mo ‹input›
      java -jar langconv.jar lang2mo ‹input› ‹output›

    === Arguments: ===

      ‹input› stands for a path (absolute or relative to current directory) pointing to:
        * a directory that contains the *.mo or *.lang files that will be converted
        * or a single file that should be converted

      ‹output› stands for a path (absolute or relative to current directory) pointing to:
        * if ‹input› is a directory: a directory into which the converted files will be written
        * if ‹input› is a file: a single file to which the conversion result will be written

      In case the ‹output› argument is omitted, the files are converted into the ‹input› directory, or a file in the same directory as the ‹input› file (depending on if ‹input› is a file or a directory).

      If a directory is given as ‹input› argument, all files in this directory (not in subdirectories) that are suitable for the conversion mode, are converted:
        * for `lang2mo` mode: all files with the file extension `*.lang`
        * for `mo2lang` mode: all files that start with the ${MoReader.BE_MAGIC.size} magic bytes of MO files (0x${MoReader.BE_MAGIC.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }} or 0x${MoReader.BE_MAGIC.reversedArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }})
  """.trimIndent())
}

fun progressBarString(percentage: Double): String = "█".repeat(percentage.toInt() / 4) +
  when (((percentage % 4) * 2).roundToInt()) {
    0 -> ""
    1 -> '▏'
    2 -> '▎'
    3 -> '▍'
    4 -> '▌'
    5 -> '▋'
    6 -> '▊'
    7 -> '▉'
    else -> '█'
  }

@ExperimentalUnsignedTypes
private enum class LangconvMode(val inputFilter: FileFilter, val readFunction: (List<File>) -> Map<String, Map<MsgId, MsgStr>>, val writeFunction: (Map<String, Map<MsgId, MsgStr>>, File) -> Unit, val isNeedsBaseLanguage: Boolean) {
  LANG2MO(
    inputFilter = FileFilter {
      it.extension == "lang"
    },
    readFunction = { inputFiles ->
      val separateInputFiles = inputFiles.partition { it.nameWithoutExtension == "en" }
      if (separateInputFiles.first.isEmpty()) {
        failWithException(IllegalArgumentException("No en.lang file is given, currently the base language is always 'en', so such a file is needed!"))
      }
      LangReader().readLangStreams("en", separateInputFiles.first.first().inputStream(), separateInputFiles.second.map { it.nameWithoutExtension to it.inputStream() }.toMap())
    },
    writeFunction = { langMap, outputDir ->
      val writer = MoWriter()
      langMap.map { File(outputDir, "${it.key}.mo").outputStream() to it.value }.forEach {
        writer.writeStream(it.first, it.second, false)
      }
    },
    isNeedsBaseLanguage = true
  ),
  MO2LANG(
    inputFilter = FileFilter {
      it.inputStream().use {
        val bytes = ByteArray(MoReader.BE_MAGIC.size)
        it.read(bytes)

        MoReader.BE_MAGIC.reversedArray().contentEquals(bytes) || MoReader.BE_MAGIC.contentEquals(bytes)
      }
    },
    readFunction = { inputFiles ->
      inputFiles.map { it.nameWithoutExtension to MoReader(it.toURI().toURL()).readFile() }.toMap()
    },
    writeFunction = { langMap, outputDir ->
      LangWriter().writeLangFile(outputDir, langMap)
    },
    isNeedsBaseLanguage = false
  )
}
