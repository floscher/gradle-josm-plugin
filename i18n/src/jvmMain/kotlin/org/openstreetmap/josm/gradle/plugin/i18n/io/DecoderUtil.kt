package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.File
import java.io.IOException

/**
 * Reads all *.lang files in a directory into a map associating the name of each language
 * with each with another map that associates strings in the base language with the corresponding translation
 * in the current language.
 * @receiver the directory for which all *.lang files directly below are decoded
 * @param baseLanguage the base language from which the translators will translate to other languages
 * @throws IOException If:
 *   * the contained files can't be listed according to [File.listFiles]
 *   * there are multiple *.lang files for the same language
 *   * there is no *.lang file for [baseLanguage] in the given directory
 */
public fun File.decodeLangFiles(baseLanguage: String): Map<String, Map<MsgId, MsgStr>> =
  listFiles { f: File ->
    f.isFile && f.extension == "lang"
  }
    .let { it ?: throw IOException("Could not list *.lang files in directory $absolutePath") }
    .map { it.nameWithoutExtension to it.readBytes() }
    .also {
      if (it.distinctBy { a -> a.first }.size < it.size) {
        throw IOException("There are duplicate languages amongst the *.lang files: ${it.sortedBy { a -> a.first }.joinToString() { a -> a.first } }")
      }
    }
    .partition { it.first == baseLanguage }
    .also { partitions ->
      if (partitions.first.size != 1) {
        throw IOException("Base language '$baseLanguage' was not found in directory $absolutePath")
      }
    }
    .let { (baseLanguages, translatedLanguages) ->
      LangFileDecoder.decodeMultipleLanguages(
        baseLanguages.first().first,
        baseLanguages.first().second,
        translatedLanguages.toMap()
      )
    }

