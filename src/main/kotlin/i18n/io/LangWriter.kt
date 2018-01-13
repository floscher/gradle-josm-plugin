package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class LangWriter {
  fun writeLangFile(langFileDir: File, languageMaps: Map<String, Map<MsgId, MsgStr>>, originLang: String) {
    // If the original language is present in the languageMaps, then use the msgids from that file.
    // Otherwise collect all the msgids from all the files.
    val originalMsgIds = languageMaps.get(originLang)?.keys ?: languageMaps.flatMap { it.value.keys }
    langFileDir.mkdirs()
    languageMaps.entries.forEach { langEntry ->
      BufferedOutputStream(FileOutputStream(File(langFileDir, "${langEntry.key}.lang"))).use { stream ->
        originalMsgIds.filter { it.id.numPlurals <= 0 }.forEach { msgid ->

          val stringBytes =
            if (langEntry.key == originLang) {
              msgid.id.singularString
            } else {
              langEntry.value.get(msgid)?.singularString
            }?.toByteArray(StandardCharsets.UTF_8)

          if (stringBytes == null) {
            stream.write(0, 0)
          } else if (stringBytes.size >= 65534) {
            throw IOException("Strings longer than 65533 bytes int UTF-8 are not supported by the *.lang file format!")
          } else if (langEntry.key != originLang && stringBytes contentEquals msgid.id.singularString.toByteArray(StandardCharsets.UTF_8)) {
            stream.write(0xFF, 0xFE)
          } else {
            stream.write(stringBytes.size.shr(8), stringBytes.size)
            stream.write(stringBytes)
          }
        }
        stream.write(0xFF, 0xFF)
        originalMsgIds.filter { it.id.numPlurals >= 1 }.forEach { msgid ->
          if (1 + msgid.id.numPlurals >= 254) {
            throw IOException("More than 253 plural forms are not supported by the *.lang file format!")
          }
          val msgstr = if (langEntry.key == originLang) msgid.id else langEntry.value.get(msgid)
          if (msgstr == null) {
            stream.write(0)
          } else if (langEntry.key != originLang && msgstr == msgid.id) {
            stream.write(0xFE)
          } else {
            stream.write(1 + msgstr.numPlurals)
            for (stringBytes in msgstr.strings.map { it.toByteArray(StandardCharsets.UTF_8) }) {
              stream.write(stringBytes.size.shr(8), stringBytes.size)
              stream.write(stringBytes)
            }
          }
        }
      }
    }
  }

  private fun OutputStream.write(vararg bytes: Int) {
    bytes.forEach { write(it) }
  }
}

