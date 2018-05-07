package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Writer for creating *.lang files from a [Map] of [MsgId]s to [MsgStr]s, which e.g. [MoReader.readFile] can produce.
 */
class LangWriter {
  /**
   * Takes translation definitions in the form of [MsgId]s and [MsgStr]s for multiple languages.
   * These are then written to a directory in the *.lang file format.
   * @param [langFileDir] the directory into which the *.lang files should be written
   * @param [languageMaps] a map with language codes as keys and maps as values.
   *   These maps associates each [MsgId] a [MsgStr], which is the translation of the [MsgId].
   * @param [originLang] the language code of the language in which the strings were written in the source code.
   * @throws IOException if writing the file is not successful
   */
  fun writeLangFile(langFileDir: File, languageMaps: Map<String, Map<MsgId, MsgStr>>, originLang: String = "en") {
    // If the original language is present in the languageMaps, then use the msgids from that file.
    // Otherwise collect all the msgids from all the files.
    val originalMsgIds = (languageMaps[originLang]?.keys
      ?: languageMaps.flatMap { it.value.keys }).filter { it.id.strings.first() != "" }
    langFileDir.mkdirs()
    languageMaps
      // Adds a *.lang file for the original language even if no *.mo or *.po file is available
      .plus(if (!languageMaps.containsKey(originLang)) mapOf(originLang to originalMsgIds.associate{ Pair(it, it.id) }) else mapOf())
      // Iterate over the languages
      .entries.forEach { langEntry ->

      BufferedOutputStream(FileOutputStream(File(langFileDir, "${langEntry.key}.lang"))).use { stream ->
        writeLangStream(stream, originalMsgIds, langEntry.value, langEntry.key == originLang)
      }
    }
  }

  fun writeLangStream(stream: OutputStream, originalMsgIds: List<MsgId>, translations: Map<MsgId, MsgStr>, isOriginLanguage: Boolean = false) {
    val originalMsgIdsPartitions = originalMsgIds.partition { it.id.strings.size <= 1 }
    // Iterate over the translatable messages in the original language without plural
    originalMsgIdsPartitions.first.forEach { msgid ->
      val stringBytes = (
        translations[msgid]?.strings?.firstOrNull()
        ?: if (isOriginLanguage) msgid.id.strings.first() else null
      )
        ?.let { if (!isOriginLanguage || msgid.context == null) it else "_:${msgid.context}\n$it" } // Prepend string with context
        ?.toByteArray(StandardCharsets.UTF_8)

      if (stringBytes == null) {
        stream.write(0, 0)
      } else if (stringBytes.size >= 65534) {
        throw IOException("Strings longer than 65533 bytes int UTF-8 are not supported by the *.lang file format!")
      } else if (!isOriginLanguage && stringBytes contentEquals msgid.id.strings.first().toByteArray(StandardCharsets.UTF_8)) {
        stream.write(0xFF, 0xFE)
      } else {
        stream.write(stringBytes.size.shr(8), stringBytes.size)
        stream.write(stringBytes)
      }
    }
    // Write the separator between singular-only and pluralized messages
    stream.write(0xFF, 0xFF)
    // Iterate over the translatable messages in the original language with plural(s)
    originalMsgIdsPartitions.second.forEach { msgid ->
      if (msgid.id.strings.size >= 254) {
        throw IOException("More than 253 plural forms are not supported by the *.lang file format!")
      }
      val msgstr = translations[msgid] ?: if (isOriginLanguage) msgid.id else null

      // If the translation is not available in the current language
      if (msgstr == null) {
        stream.write(0)
        // If the file currently written is not the one for the original language and if the translated string is the same as the original
      } else if (!isOriginLanguage && msgstr == msgid.id) {
        stream.write(0xFE)
      } else {
        // Write the number of forms (singular form plus one or more plural forms)
        stream.write(msgstr.strings.size)
        // For each form write the size as 2 bytes, then write the string in UTF-8 encoding
        msgstr.strings
          .mapIndexed { i, string ->
            string
              .let{if (!isOriginLanguage || i >= 1 || msgid.context == null) it else "_:${msgid.context}\n$it" }
              .toByteArray(StandardCharsets.UTF_8)
          }
          .forEach { stringBytes ->
            if (stringBytes.size >= 65534) {
              throw IOException("Strings longer than 65533 bytes in UTF-8 are not supported by the *.lang file format!")
            }
            stream.write(stringBytes.size.shr(8), stringBytes.size)
            stream.write(stringBytes)
          }
      }
    }
  }

  /**
   * Convenience method when you have two write multiple hardcoded bytes.
   * Calls [write] once for each parameter given to this method.
   * Note that the bytes are given as [Int]s, but everything except the 8 lowest bits for each value will be ignored.
   */
  private fun OutputStream.write(vararg bytes: Int) {
    bytes.forEach { write(it) }
  }
}

