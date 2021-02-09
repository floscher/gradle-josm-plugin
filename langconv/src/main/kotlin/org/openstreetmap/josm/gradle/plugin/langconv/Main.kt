package org.openstreetmap.josm.gradle.plugin.langconv

import org.openstreetmap.josm.gradle.plugin.i18n.io.MoFileFormat
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoFileEncoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Locale
import kotlin.system.exitProcess
import org.openstreetmap.josm.gradle.plugin.i18n.io.encodeToLangFiles
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangFileDecoder

@ExperimentalUnsignedTypes
@Throws(IllegalArgumentException::class)
fun main(vararg args: String) {
  if (args.isEmpty()) {
    printUsage()
    exitProcess(0)
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
        it.listFiles(mode.inputFilter)?.toList()
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
    failWithException(IllegalArgumentException("The argument given as output directory is not a directory!: ${outputDir.safeCanonicalPath()}"))
  }

  val strings = mode.readFunction.invoke(inputFiles)

  try {
    println(strings.getTranslationStatsString("en", mode.isNeedsBaseLanguage) { it.value.size })
  } catch (e: IllegalArgumentException) {
    failWithException(e)
  }

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
        * for `mo2lang` mode: all files that start with the ${MoFileFormat.BE_MAGIC.size} magic bytes of MO files (0x${MoFileFormat.BE_MAGIC.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }} or 0x${MoFileFormat.BE_MAGIC.reversed().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }})
  """.trimIndent())
}

@ExperimentalUnsignedTypes
private enum class LangconvMode(val inputFilter: FileFilter, val readFunction: (List<File>) -> Map<String, Map<MsgId, MsgStr>>, val writeFunction: (Map<String, Map<MsgId, MsgStr>>, File) -> Unit, val isNeedsBaseLanguage: Boolean) {
  LANG2MO(
    inputFilter = FileFilter {
      it.isFile &&
      it.extension == "lang"
    },
    readFunction = { inputFiles ->
      val separateInputFiles = inputFiles.partition { it.nameWithoutExtension == "en" }
      if (separateInputFiles.first.isEmpty()) {
        failWithException(IllegalArgumentException("No en.lang file is given, currently the base language is always 'en', so such a file is needed!"))
      }

      LangFileDecoder.decodeMultipleLanguages("en", separateInputFiles.first.first().readBytes(), separateInputFiles.second.map { it.nameWithoutExtension to it.readBytes() }.toMap())
    },
    writeFunction = { langMap, outputDir ->
      langMap.map { File(outputDir, "${it.key}.mo").outputStream() to it.value }.forEach {
        it.first.write(MoFileEncoder.LITTLE_ENDIAN.encodeToByteArray(it.second))
      }
    },
    isNeedsBaseLanguage = true
  ),
  MO2LANG(
    inputFilter = FileFilter {
      it.isFile &&
      it.inputStream().use {
        val bytes = ByteArray(MoFileFormat.BE_MAGIC.size)
        it.read(bytes)

        MoFileFormat.BE_MAGIC.reversed() == bytes.toList() || MoFileFormat.BE_MAGIC == bytes.toList()
      }
    },
    readFunction = { inputFiles ->
      inputFiles.map { it.nameWithoutExtension to MoFileDecoder.decodeToTranslations(it.readBytes()) }.toMap()
    },
    writeFunction = ::encodeToLangFiles,
    isNeedsBaseLanguage = false
  )
}
