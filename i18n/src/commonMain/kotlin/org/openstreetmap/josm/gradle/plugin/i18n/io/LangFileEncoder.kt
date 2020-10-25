package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Takes translations for multiple languages and converts them into the *.lang file format used by JOSM.
 *
 * If [translations] contains a mapping for [baseLanguage], only translations for the [MsgId]s from that [Map.Entry] will be encoded.
 *
 * But if [translations] does not contain a mapping for [baseLanguage], all [MsgId]s from all entries of [translations] will be encoded.
 * @param translations a Map that maps language codes to Maps that contain the translations for the language code
 * @param baseLanguage the base language, from which all the strings were translated to other languages
 */
public fun encodeToLangBytes(translations: Map<String, Map<MsgId, MsgStr>>, baseLanguage: String = LangFileEncoder.DEFAULT_BASE_LANGUAGE): Map<String, ByteArray> =
  LangFileEncoder(translations[baseLanguage]?.keys?.toList() ?: translations.values.flatMap { it.keys })
    .let { encoder ->
      translations.minus(baseLanguage).entries.associate {
        it.key to encoder.encodeToByteArray(it.value)
      }.plus(baseLanguage to encoder.encodeToBaseLanguageByteArray())
    }

/**
 * Encoder for the *.lang file format used by JOSM.
 * @param baseMsgIds the [MsgId]s in the base language for which translations will be encoded
 */
public class LangFileEncoder(baseMsgIds: List<MsgId>): I18nFileEncoder {
  public companion object {
    /**
     * The language code `en` for English, which is considered to be the base language by default,
     * if not otherwise stated by passing a different argument as parameter to e.g. [encodeToLangBytes].
     */
    public val DEFAULT_BASE_LANGUAGE: String = "en"
    /** the separator between singular-only and pluralized messages */
    private val SINGULAR_PLURAL_SEPARATOR: List<Byte> = listOf(0xFF, 0xFF).map { it.toByte() }
  }

  private val singularMsgIds: List<MsgId>
  private val pluralMsgIds: List<MsgId>

  init {
    val baseMsgIdPartitions = baseMsgIds.distinct().minus(GETTEXT_HEADER_MSGID).partition { it.id.strings.size <= 1 }
    singularMsgIds = baseMsgIdPartitions.first
    pluralMsgIds = baseMsgIdPartitions.second
  }

  /**
   * Converts the [MsgId]s of the base language (see constructor parameter) to the *.lang file format.
   * @return a [ByteArray] with the contents of the *.lang file for the base language
   */
  public fun encodeToBaseLanguageByteArray(): ByteArray =
    singularMsgIds.flatMap{ it.encodeWithSizeAndContext() }
      .plus(SINGULAR_PLURAL_SEPARATOR)
      .plus(
        pluralMsgIds.flatMap {
          it.id.encodePluralSize().plus(it.encodeWithSizeAndContext())
        }
      )
      .toByteArray()

  private fun MsgStr.encodePluralSize(): List<Byte> {
    val numForms = strings.size
    return if (numForms >= 254) {
      throw IllegalArgumentException("The *.lang format “only” supports up to 253 grammatical numbers (singular and plural forms)! $numForms forms were provided.")
    } else {
      listOf(numForms.toByte())
    }
  }

  private fun MsgId.encodeWithSizeAndContext(): List<Byte> =
    id.strings.mapIndexed { i, string -> if (i >= 1 || context == null) string else "_:${context}\n$string" }
      .encodeWithLength()

  private fun List<String>.encodeWithLength(): List<Byte> = flatMap { string ->
    string.encodeToByteArray().toList()
      .let { stringBytes ->
        val byteSize = stringBytes.size
        if (byteSize >= 65534) { // 0xFFFE and 0xFFFF are reserved values
          throw IllegalArgumentException("The *.lang format only supports strings up to 65533 UTF-8 bytes in length! $")
        }
        listOf(byteSize.shr(8).toByte(), byteSize.toByte()).plus(stringBytes)
      }
  }

  /**
   * Converts translations for one language to the *.lang file format.
   * Do not use this for the base language! Use [encodeToBaseLanguageByteArray] instead!
   * @param translations a mapping from [MsgId] in the base language to [MsgStr] in the target language.
   *    Any map entry for a [MsgId] that **was not** passed with the constructor parameter of [LangFileEncoder] will be **disregarded**.
   *    Any [MsgId] that **was** passed with the constructor parameter of [LangFileEncoder], but does not appear in this [Map] **will** be encoded as having no translation.
   * @return the *.lang file for the translated language (not the base language), as a [ByteArray]
   */
  override fun encodeToByteArray(translations: Map<MsgId, MsgStr>): ByteArray =
    singularMsgIds.flatMap { msgid ->
      val msgstr = translations[msgid]
      when (msgstr) {
        null -> listOf(0.toByte(), 0.toByte())
        msgid.id -> listOf(0xFF.toByte(), 0xFE.toByte())
        else -> msgstr.strings.encodeWithLength()
      }
    }
      .plus(SINGULAR_PLURAL_SEPARATOR)
      .plus(
        pluralMsgIds.flatMap { msgid ->
          val msgstr = translations[msgid]
          when (msgstr) {
            null -> listOf(0.toByte())
            msgid.id -> listOf(0xFE.toByte())
            else -> msgstr.encodePluralSize().plus(msgstr.strings.encodeWithLength())
          }
        }
      )
      .toByteArray()
}
