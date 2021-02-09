package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoFileEncoder.Companion.toBytes

@ExperimentalUnsignedTypes
class I18nReadWriteTest {

  val emptyTranslations: Map<MsgId, MsgStr> = mapOf(GETTEXT_DEFAULT_HEADER)
  val translations1: Map<MsgId, MsgStr> = mapOf(
    GETTEXT_HEADER_MSGID to MsgStr("Sing\nSing2\n$GETTEXT_CONTENT_TYPE_UTF8\n"),
    MsgId(MsgStr("1", "2")) to MsgStr("Sing"),
    MsgId(MsgStr("1", "2"), "context") to MsgStr("Singular", "Plural"),
    MsgId(MsgStr("Many plurals (253 is maximum of *.lang)", "2")) to MsgStr("1", *(2..253).map { it.toString() }.toTypedArray()),
    MsgId(MsgStr("Emoji \uD83D\uDE0D", "\uD83C\uDDF1\uD83C\uDDFB"), "\uD83D\uDE39") to MsgStr("\uD83E\uDDB8\uD83C\uDFFF\u200D♂️", "\uD83C\uDFF3️\u200D\uD83C\uDF08", ""),
    MsgId(MsgStr("Umlaut äöüÄÖÜ")) to MsgStr("ẞß"),
    MsgId(MsgStr("Special escape chars")) to MsgStr("\u0007\u0008\u000C\n\r\t\u000B\\\"")
  )

  val asciiTranslations = mapOf(
    GETTEXT_DEFAULT_HEADER,
    MsgId(MsgStr("A")) to MsgStr("A+"),
    MsgId(MsgStr("XYZ42")) to MsgStr("XYZ42+"),
    MsgId(MsgStr("context"), "context*") to MsgStr("context+"),
    MsgId(MsgStr("plural", "plural~")) to MsgStr("plural+"),
    MsgId(MsgStr("pluralContext", "pluralContext~"), "pluralContext*") to MsgStr("pluralContext+", "pluralContext2+"),
    MsgId(MsgStr("ABC")) to MsgStr("ABC"),
    MsgId(MsgStr("ABC", "ABC")) to MsgStr("ABC", "ABC"),
    MsgId(MsgStr("ABC", "ABC"), "ABC") to MsgStr("ABC", "ABC", "ABC"),
  )

  val dummyTranslationsEn: Map<String, Map<MsgId, MsgStr>> = LangFileDecoderTest().getDummyTranslations("en")
  val dummyTranslationsRu: Map<String, Map<MsgId, MsgStr>> = LangFileDecoderTest().getDummyTranslations("ru")

  @Test
  fun testMoSerializationPersistence() {
    testMoSerializationPersistence(emptyTranslations, "empty")
    testMoSerializationPersistence(translations1, "translations1")
    testMoSerializationPersistence(asciiTranslations, "ascii")
    dummyTranslationsEn.forEach { (language, translations) ->
      testMoSerializationPersistence(translations.plus(GETTEXT_DEFAULT_HEADER), "dummy-$language")
    }
  }

  private fun testMoSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    testMoSerializationPersistence(translations, true, name)
    testMoSerializationPersistence(translations, false, name)
  }

  private fun testMoSerializationPersistence(translations: Map<MsgId, MsgStr>, isBigEndian: Boolean, name: String) {
    val writeResult1 = MoFileEncoder.getInstance(isBigEndian).encodeToByteArray(translations)

    val readResult1 = MoFileDecoder.decodeToTranslations(writeResult1)
    val writeResult2 = MoFileEncoder.getInstance(isBigEndian).encodeToByteArray(readResult1)
    val readResult2 = MoFileDecoder.decodeToTranslations(writeResult2)

    assertEquals(translations, readResult1)
    assertEquals(readResult1, readResult2)
    assertEquals(writeResult1.toList(), writeResult2.toList())

    assertEquals(I18nReadWriteTest::class.java.getResource("mo/$name-${if (isBigEndian) "BE" else "LE"}.mo")?.readBytes()?.toList(), writeResult1.toList()) {
      "Contents of mo/$name-${if (isBigEndian) "BE" else "LE"}.mo are not generated as expected!"
    }
  }

  @Test
  fun testPoSerializationPersistence() {
    testPoSerializationPersistence(emptyTranslations, "empty")
    testPoSerializationPersistence(translations1, "translations1")
    testPoSerializationPersistence(asciiTranslations, "ascii")
    dummyTranslationsEn.forEach { (language, translations) ->
      testPoSerializationPersistence(translations.plus(GETTEXT_DEFAULT_HEADER), "dummy-$language")
    }
  }

  private fun testPoSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    val bytes = PoFileEncoder.encodeToByteArray(translations)
    assertEquals(translations, PoFileDecoder.decodeToTranslations(bytes)) {
      "Contents of po/$name.po are not parsed as expected!"
    }
    assertEquals(I18nReadWriteTest::class.java.getResource("po/$name.po")?.readBytes()?.decodeToString(), bytes.decodeToString()) {
      "Contents of po/$name.po are not generated as expected!"
    }
  }

  @Test
  fun testLangSerializationPersistence() {
    testLangSerializationPersistence(emptyTranslations, "empty")
    testLangSerializationPersistence(translations1, "translations1")
    testLangSerializationPersistence(asciiTranslations, "ascii")

    val langFileEncoderEn = LangFileEncoder(dummyTranslationsEn["en"]?.map { it.key }!!)
    val dummyEnLangBytes = dummyTranslationsEn.map { (language, translations) ->
      val bytes = if (language == "en") langFileEncoderEn.encodeToBaseLanguageByteArray() else langFileEncoderEn.encodeToByteArray(translations)
      assertEquals(I18nReadWriteTest::class.java.getResource("lang/dummy-baseEn-$language.lang")?.readBytes()?.toList(), bytes.toList()) {
        "lang/dummy-baseEn-$language.lang not generated as expected!"
      }

      language to bytes
    }.toMap()

    assertEquals(dummyTranslationsEn, LangFileDecoder.decodeMultipleLanguages("en", dummyEnLangBytes["en"]!!, dummyEnLangBytes.minus("en")))

    val langFileEncoderRu = LangFileEncoder(dummyTranslationsRu["ru"]?.map { it.key }!!)
    val dummyRuLangBytes = dummyTranslationsRu.map { (language, translations) ->
      val bytes = if (language == "ru") langFileEncoderRu.encodeToBaseLanguageByteArray() else langFileEncoderRu.encodeToByteArray(translations)
      assertEquals(I18nReadWriteTest::class.java.getResource("lang/dummy-baseRu-$language.lang")?.readBytes()?.toList(), bytes.toList()) {
        "lang/dummy-baseRu-$language.lang not generated as expected!"
      }

      language to bytes
    }.toMap()
    assertEquals(dummyTranslationsRu, LangFileDecoder.decodeMultipleLanguages("ru", dummyRuLangBytes["ru"]!!, dummyRuLangBytes.minus("ru")))
  }

  private fun testLangSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    val encoder = LangFileEncoder(translations.map { it.key })
    val langBaseBytes = encoder.encodeToBaseLanguageByteArray()
    val langBytes = encoder.encodeToByteArray(translations)

    assertEquals(I18nReadWriteTest::class.java.getResource("lang/$name-en.lang")?.readBytes()?.toList(), langBaseBytes.toList()) {
      "lang/$name-en.lang not generated as expected!"
    }
    assertEquals(I18nReadWriteTest::class.java.getResource("lang/$name-xy.lang")?.readBytes()?.toList(), langBytes.toList()) {
      "lang/$name-xy.lang not generated as expected!"
    }

    assertEquals(
      translations.filter { it.key.id.strings.any { it.isNotEmpty() } },
      LangFileDecoder(langBaseBytes).decodeToTranslations(langBytes)
    )
  }

  @Test
  fun testMoToLangAndBack() {
    testMoToLangAndBack(emptyTranslations)
    testMoToLangAndBack(translations1)
    testMoToLangAndBack(asciiTranslations)
  }

  fun testMoToLangAndBack(translations: Map<MsgId, MsgStr>) {
    val moWriteResult1 = MoFileEncoder.BIG_ENDIAN.encodeToByteArray(translations)

    val moReadResult1 = MoFileDecoder.decodeToTranslations(moWriteResult1)
    // The empty string is not envisaged in the *.lang file format, so it is extracted here and put back into the result later
    val emptyElement = moReadResult1.keys.firstOrNull { it.toBytes().isEmpty() }
    assertEquals(translations, moReadResult1)

    val encoder = LangFileEncoder(moReadResult1.keys.toList())
    val baseLanguageBytes = encoder.encodeToBaseLanguageByteArray()
    val otherLanguageBytes = encoder.encodeToByteArray(moReadResult1)

    val langReadResult = LangFileDecoder(baseLanguageBytes).decodeToTranslations(otherLanguageBytes)
      // putting the empty element back into the result (if present)
      .plus(listOfNotNull(emptyElement).map { it to moReadResult1[it] })

    assertEquals(moReadResult1, langReadResult)
    assertNotNull(langReadResult)

    val moWriteResult2 = MoFileEncoder.LITTLE_ENDIAN.encodeToByteArray(langReadResult.mapNotNull{ val value = it.value; if (value != null) it.key to value else null }.toMap())

    val moReadResult2 = MoFileDecoder.decodeToTranslations(moWriteResult2)
    assertEquals(langReadResult, moReadResult2)
  }
}
