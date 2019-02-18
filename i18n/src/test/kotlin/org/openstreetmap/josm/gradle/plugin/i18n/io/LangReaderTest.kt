package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

val DUMMY_TRANSLATIONS: Map<String?, List<Map<String, MsgStr>>> = mapOf(
  null to listOf(
    mapOf(
      "de" to MsgStr("\uD83C\uDDE9\uD83C\uDDEA"),
      "en" to MsgStr("\uD83C\uDDEC\uD83C\uDDE7"),
      "fr" to MsgStr("\uD83C\uDDEB\uD83C\uDDF7"),
      "ru" to MsgStr("\uD83C\uDDF7\uD83C\uDDFA")
    ),
    mapOf(
      "de" to MsgStr("Deutsch"),
      "en" to MsgStr("English"),
      "en_US" to MsgStr("American English"),
      "fr" to MsgStr("Français"),
      "ru" to MsgStr("Русский")
    ),
    mapOf(
      "de" to MsgStr("Text ohne Kontext"),
      "en" to MsgStr("String without context"),
      "fr" to MsgStr("Texte sans contexte"),
      "ru" to MsgStr("Текст без контекста")
    ),
    mapOf(
      "de" to MsgStr("de1", "de2"),
      "en" to MsgStr("en1", "en2"),
      "fr" to MsgStr("fr1", "fr2", "fr3"),
      "ru" to MsgStr("ru1", "ru2", "ru3", "ru4")
    ),
    mapOf("de" to MsgStr("nur Deutsch")),
    mapOf("en" to MsgStr("only English")),
    mapOf("fr" to MsgStr("seulement français")),
    mapOf("ru" to MsgStr("только русский")),
    mapOf(
      "de" to MsgStr("en-de-trans"),
      "en" to MsgStr("en-de-orig")
    ),
    mapOf(
      "fr" to MsgStr("en-fr-trans"),
      "en" to MsgStr("en-fr-orig")
    ),
    mapOf(
      "ru" to MsgStr("en-ru-trans"),
      "en" to MsgStr("en-ru-orig")
    ),
    listOf("de", "en", "en_US", "fr", "ru").associate { it to MsgStr("Always the same") },
    listOf("de", "en", "en_US", "fr", "ru").associate { it to MsgStr("Always the same", "plural") }
  ),
  "context" to listOf(
    mapOf(
      "de" to MsgStr("Text mit Kontext"),
      "en" to MsgStr("Text with context"),
      "fr" to MsgStr("Texte avec contexte"),
      "ru" to MsgStr("Текст с контекстом")
    ),
    listOf("de", "en", "en_US", "fr", "ru").associate { it to MsgStr("Always the same", "plural") }
  )
)

class LangReaderTest {

  @Test
  fun testEmptyFiles() {
    val baseLang = "en"
    val languages = listOf("de", "fr").plus(baseLang)
    val emptyResult = languages.associate { it to mapOf<MsgId, MsgStr>() }

    // Empty
    testLangStreams(baseLang, languages, { ByteArray(0) }) {
      assertEquals(emptyResult, it.invoke())
    }

    testLangStreams(
      baseLang,
      languages,
      {
        when(it) {
          "en" -> ByteArray(0)
          else -> ByteArray(4) { 0.toSignedByte() }
        }
      }
    ) {
      assertEquals(emptyResult, it.invoke())
    }

    // 0xFFFF
    testLangStreams(baseLang, languages, { ByteArray(2) { 0xFF.toSignedByte() } }) {
      assertEquals(emptyResult, it.invoke())
    }

    // 0xFFFFFF
    testLangStreams(baseLang, languages, { ByteArray(3) { 0xFF.toSignedByte() } }) {
      assertEquals(emptyResult, it.invoke())
    }
  }

  @Test
  fun test1Byte() {
    testLangStreams(
      "en",
      listOf("en"),
      { byteArrayOf(0xFF.toSignedByte()) },
      { assertThrows(IOException::class.java) { it.invoke() } }
    )
  }

  @Test
  fun testLongBaseString() {
    val languages = listOf("en", "it", "es", "ca-valencia")
    testLangStreams(
      languages[0],
      languages,
      {
        ByteArray(65535) {
          when (it) {
            0 -> 0xFF.toSignedByte()
            1 -> 0xFD.toSignedByte()
            else -> 'a'.toByte()
          }
        }
      },
      {
        assertEquals(
          languages.associate { it to mapOf(MsgId(MsgStr("a".repeat(65533))) to MsgStr("a".repeat(65533))) },
          it.invoke()
        )
      }
    )

    testLangStreams(languages[0], languages,
      {
        ByteArray(65536) {
          when (it) {
            0 -> 0xFF.toSignedByte()
            1 -> 0xFE.toSignedByte()
            else -> 'a'.toByte()
          }
        }
      },
      { assertThrows(IOException::class.java) { it.invoke() } }
    )
  }

  @Test
  fun testWriteReadLang() {
    val msgids = mutableListOf<MsgId>()
    msgids.add(MsgId(MsgStr("ASCII")))
    msgids.add(MsgId(MsgStr("Umlaut: ÄÖÜäöüß")))
    msgids.add(MsgId(MsgStr("Smiley: \uD83D\uDE09")))
    msgids.add(MsgId(MsgStr("Flag: \uD83C\uDDEC\uD83C\uDDE7")))
    msgids.add(MsgId(MsgStr("Singular", "Plural")))
    msgids.add(MsgId(MsgStr("Singular", "Plural1", "Plural 2", "Plural 3", "Plural 4")))

    msgids.add(MsgId(MsgStr("String with context"), "I'm the context"))
    msgids.add(MsgId(MsgStr("String", "Plural1", "Plural 2", "Plural 3", "Plural 4"), "Context"))

    println("Test writing and then reading the following MsgIds to the *.lang file format:")
    msgids.forEach { println(it) }

    val outStream = ByteArrayOutputStream()
    LangWriter().writeLangStream(outStream, msgids, mapOf(* msgids.map { Pair(it, it.id) }.toTypedArray()), true)

    printBytesAsHex(outStream.toByteArray())

    assertEquals(msgids.toSet(), LangReader().readBaseLangStream(ByteArrayInputStream(outStream.toByteArray())).toSet())
  }

  @Test
  fun testWriteReadMultiLang() {

    val baseLang = "en"

    val translatableStrings = getDummyTranslatableStrings(baseLang)
    val translationsByLang = getDummyTranslations(baseLang)
    val langStreams = translationsByLang.keys.associate { it to ByteArrayOutputStream() }
    val writer = LangWriter()
    langStreams.forEach {
      println("Writing language ${it.key}…")
      println("Translatable: " + translatableStrings.joinToString("\n"))
      println("Translated: " + translationsByLang.getValue(it.key).values.joinToString("\n"))
      writer.writeLangStream(it.value, translatableStrings, translationsByLang.getValue(it.key), it.key == baseLang)
      println("Language is ${it.key}")
      printBytesAsHex(it.value.toByteArray())
    }

    val translationsByLang2 = LangReader().readLangStreams(
      baseLang,
      ByteArrayInputStream(langStreams.getValue(baseLang).toByteArray()),
      langStreams.filterNot { it.key == baseLang }.map { it.key to ByteArrayInputStream(it.value.toByteArray()) }.toMap())


    assertEquals(translationsByLang, translationsByLang2)
  }

  private fun printBytesAsHex(bytes: ByteArray) {
    val byteString = StringBuilder()
    for (i in 0 until bytes.size) {
      if (i % 50 == 0) {
        byteString.append("\n${i/50}")
      }
      byteString.append(bytes[i].toUnsignedInt().toString(16).padStart(2, '0')).append(' ')
    }

    println("\nBytes as Hex (50 per line):\n$byteString")
    println("\nThe bytes as characters:\n${String(bytes, Charsets.UTF_8)}")
  }

  private fun testLangStreams(baseLang: String, languages: List<String>, inputBytes: (String) -> ByteArray, test: (() -> Map<String, Map<MsgId, MsgStr>>) -> Unit) {
    test.invoke {  LangReader().readLangStreams(baseLang, ByteArrayInputStream(inputBytes.invoke(baseLang)), languages.associate { it to ByteArrayInputStream(inputBytes.invoke(it)) }) }
  }

  private fun getDummyLangStreamsWithBytes(baseLang: String, languages: List<String>, langToBytes: (String) -> ByteArray): Pair<InputStream, Map<String, InputStream>> {
    return ByteArrayInputStream(langToBytes.invoke(baseLang)) to languages.associate { it to ByteArrayInputStream(langToBytes.invoke(it)) }
  }

  private fun getDummyTranslatableStrings(baseLang: String) = DUMMY_TRANSLATIONS.flatMap { contextEntry ->
    contextEntry.value
      .mapNotNull { it[baseLang] }
      .map { MsgId(it, contextEntry.key) }
  }

  private fun getDummyTranslations(baseLang: String) = DUMMY_TRANSLATIONS.flatMap { it.value.flatMap { it.keys } }.toSet()
    .associate { lang ->
      Pair(
        lang,
        DUMMY_TRANSLATIONS.flatMap { contextEntry ->
          contextEntry.value
            .mapNotNull {
              val baseLangStr = it[baseLang]
              val transLangStr = it[lang]
              if (baseLangStr == null || transLangStr == null) {
                null
              } else {
                MsgId(baseLangStr, contextEntry.key) to transLangStr
              }
            }
        }.toMap()
      )
    }

  private fun Int.toSignedByte(): Byte {
    return (if (this > 127) this - 256 else this).toByte()
  }

  private fun Byte.toUnsignedInt(): Int {
    return if (this < 0) this + 256 else this.toInt()
  }
}
