package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A reader class for reading the custom *.lang file format of JOSM.
 */
class LangReader {

  /**
   * Reads all *.lang files in a directory into a map from the name of each language
   * to a map that associates the string in the base language with the string translated to the current language.
   * @param langFileDir the directory containing the *.lang files
   * @param baseLang the base language from which the translators will translate to other languages
   */
  @Throws(IOException::class)
  fun readLangFiles(langFileDir: File, baseLang: String): Map<String, Map<MsgId, MsgStr>> {
    val langFiles = langFileDir
      .listFiles { file -> file.isFile && file.extension == "lang" }
      ?.partition { it.name == "$baseLang.lang" }

    require(langFiles != null && langFiles.first.isNotEmpty()) {
      "The base language '$baseLang' is not present among the language files in '${langFileDir.absolutePath}'!"
    }

    return readLangStreams(
      baseLang,
      langFiles.first.first().inputStream(),
      langFiles.second.map {
        it.nameWithoutExtension to it.inputStream()
      }.toMap()
    )
  }

  /**
   * Read translations from *.lang files available as [InputStream]s
   * @param baseLang the base language, from where the translatable strings originate
   * @param baseStream the [InputStream] for the *.lang file of the base language
   * @param langStreams a [Map] containing the [InputStream]s for the other languages
   *   (the keys of this map are the names of the *.lang files without extension)
   */
  @Throws(IOException::class)
  fun readLangStreams(baseLang: String, baseStream: InputStream, langStreams: Map<String, InputStream>): Map<String, Map<MsgId, MsgStr>> {
    val baseStrings = readBaseLangStream(baseStream)
    return langStreams.map { (lang, stream) ->
      lang to readTranslatedLang(stream, baseStrings)
        .mapIndexedNotNull { index, msgStr ->
          if (msgStr == null) {
            null
          } else {
          baseStrings[index] to msgStr
          }
        }.toMap()
    }.toMap().plus(baseLang to baseStrings.associate { it to it.id })
  }

  /**
   * Reads the strings of the base language from the input stream.
   * @return a [List] of all [MsgId]s that can be read from the [InputStream]
   * @throws [IOException] if the data in the [InputStream] is not in the *.lang format
   */
  @Throws(IOException::class)
  fun readBaseLangStream(stream: InputStream) = readLangStream(stream) {
    if (it.isEmpty()) {
      throw IOException("A MsgId must have one or more strings (${it.size} given)!")
    }
    val newlineIndex = it[0].indexOf('\n')
    if (!it[0].startsWith("_:") || newlineIndex <= 1) {
      MsgId(MsgStr(it))
    } else {
      MsgId(
        MsgStr(it[0].substring(newlineIndex + 1), * it.subList(1, it.size).toTypedArray()),
        it[0].substring(2, newlineIndex)
      )
    }
  }.map { it ?: throw IOException("Entries for the base language must not be null!") }

  private fun readTranslatedLang(stream: InputStream, baseStrings: List<MsgId>) = readLangStream(stream, baseStrings) { MsgStr(it) }

  @Throws(IOException::class)
  private fun <T> readLangStream(stream: InputStream, baseStrings: List<MsgId>? = null, stringsToResult: (List<String>) -> T): List<T?> {
    val result = mutableListOf<T?>()
    stream.use {
      var finishedSingularStrings = false
      var finishedPluralStrings = false

      while (!finishedSingularStrings) {
        val stringLength = it.readTwoBytesAsInt()
        when (stringLength) {
          in Int.MIN_VALUE..-1 -> { // File ended
            finishedSingularStrings = true
            finishedPluralStrings = true
          }
          0 -> if (baseStrings == null) {
            throw IOException("The file indicates this string is not translated. This is not allowed for the base language!")
          } else {
            result.add(null)
          }
          0xFFFF /* = 65535 */ -> finishedSingularStrings = true
          0xFFFE /* = 65534 */ -> if (baseStrings == null) {
            throw IOException("The length of the original string must be 65533 or shorter! Make sure the original language is set correctly!")
          } else {
            // Special value: 0xFFFE means same as in base language
            result.add(stringsToResult.invoke(baseStrings[result.size].id.strings))
          }
          else -> {
            val stringBytes =  ByteArray(stringLength)
            it.readAllOrException(stringBytes)
            result.add(stringsToResult.invoke(listOf(String(stringBytes, Charsets.UTF_8))))
          }
        }
      }
      while (!finishedPluralStrings && it.available() >= 1) {
        val numPlurals = it.read()
        when(numPlurals) {
          in Int.MIN_VALUE..-1,
          in 0xFF..Int.MAX_VALUE -> {
              // not officially documented what 0xFF means, I assume end of plural strings, ignore all following bytes
              finishedPluralStrings = true
            }
          0 -> if (baseStrings == null) {
              throw IOException("The file indicates this string is not translated. This is not allowed for the original language!")
            } else {
              result.add(null)
            }
          0xFE -> if (baseStrings == null) {
              throw IOException("The number of singular/plural forms for a given string must be 253 or lower!")
            } else {
              // special value: 0xFE means same as in base language
              result.add(stringsToResult.invoke(baseStrings[result.size].id.strings))
            }
          else -> {
            val strings = mutableListOf<String>()
            for (p in 0 until numPlurals) {
              val stringLength = it.readTwoBytesAsInt()
              when (stringLength) {
                in Integer.MIN_VALUE..-1 -> throw IOException("The *.lang file ended unexpectedly!")
                else -> {
                  val stringBytes = ByteArray(stringLength)
                  it.readAllOrException(stringBytes)
                  strings.add(stringBytes.toString(Charsets.UTF_8))
                }
              }
            }

            result.add(stringsToResult.invoke(strings))
          }
        }
      }
    }

    return result
  }
}
