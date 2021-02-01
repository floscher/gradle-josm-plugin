package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A decoder class for reading the custom *.lang file format of JOSM.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public class LangFileDecoder(baseLanguageBytes: ByteArray): I18nFileDecoder {

  public companion object {
    private const val ERROR_MESSAGE_NONBASE_UNTRANSLATED = "This is not a base language!" +
      "There is a string in this supposed base language that indicates it is not translated from the base language (which does not make sense)."
    private const val ERROR_MESSAGE_NONBASE_SAME_AS_BASE = "This is not a base language!" +
      "There is a string in this supposed base language that indicates it is the same as in the base language (which does not make sense)."

    /**
     * Decodes an entire set of *.lang files at once.
     * This is equivalent to creating a [LangFileDecoder] for the base language and then
     * calling [decodeToTranslations] for the rest of the languages.
     * @param baseLanguage the base language for the messages
     * @param baseLanguageBytes the bytes of the *.lang file for the [baseLanguage]
     * @param otherLanguages a map associating the other language identifiers with the bytes of their *.lang files
     * @return a Map associating all language identifiers (including the base language) to a Map associating
     *   the base messages to the translated messages in the different languages.
     */
    public fun decodeMultipleLanguages(baseLanguage: String, baseLanguageBytes: ByteArray, otherLanguages: Map<String, ByteArray>): Map<String, Map<MsgId, MsgStr>> {
      val decoder = LangFileDecoder(baseLanguageBytes)
      return otherLanguages
        .map { (language, languageBytes) -> language to decoder.decodeToTranslations(languageBytes) }
        .plus(baseLanguage to decoder.baseMessages.map { it to it.id }.toMap())
        .toMap()
    }
  }

  /**
   * The [MsgId]s found in the base language file.
   * They are in the order encountered in the *.lang file. Keep in mind, the order is important for *.lang files.
   */
  public val baseMessages: List<MsgId> = decodeTranslations(baseLanguageBytes) { msgid, _ ->
    val firstString = msgid.id.strings[0]

    val newlineIndex = firstString.indexOf('\n')
    if (firstString.startsWith("_:") && newlineIndex > 1) {
      MsgId(
        id = MsgStr(
          firstString.substring(newlineIndex + 1),
          * msgid.id.strings.subList(1, msgid.id.strings.size).toTypedArray()
        ),
        context = firstString.substring(2, newlineIndex)
      )
    } else msgid
  }

  /**
   * Decodes a *.lang file for a translated language.
   * To decode the base language, pass the bytes of the base *.lang file to the [LangFileDecoder] constructor
   * and find the contained [MsgId]s in [baseMessages].
   * @param bytes the bytes of the *.lang file to be decoded
   * @return a [Map] associating all messages in the base language for which there is a translation to the current language with the message translated to the current language
   */
  public override fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr> =
    decodeTranslations(bytes, baseMessages) { id, str ->
      if (str == null) null else id to str
    }
      .filterNotNull()
      .toMap()

  /**
   * This is a helper method that can read base language files or translated language files, depending on the parameters.
   *
   * @param bytes the bytes of the *.lang file that are being decoded
   * @param baseStrings A list of all the [MsgId]s of the base language.
   *   Iff decoding the base language, use `null` for this parameter.
   * @param transformToListElement a function that returns an element for the returned list based on a [MsgId] and a [MsgStr].
   * @return a list of all decoded entries. For each [MsgId]/[MsgStr] pair that is found,
   *   [transformToListElement] is applied to both and the result is added to the list that is returned
   */
  private fun <T: Any?> decodeTranslations(bytes: ByteArray, baseStrings: List<MsgId>? = null, transformToListElement: (MsgId, MsgStr?) -> T): List<T> {
    var currentByteIndex = 0
    var isSingularsComplete = false
    var isPluralsComplete = false
    val resultList = mutableListOf<T>()

    // Singular-only strings
    while (!isSingularsComplete && (baseStrings == null || resultList.size < baseStrings.size)) {
      val currentBaseString = baseStrings?.get(resultList.size)
      val stringLength = bytes.decodeTwoByteIntAt(currentByteIndex)
      currentByteIndex += 2
      when (stringLength) {
        null -> {
          // file ended
          isSingularsComplete = true
          isPluralsComplete = true
        }
        0xFFFF /* = 65535 */ -> // end of singular-only section
          isSingularsComplete = true
        0xFFFE /* = 65534 */ -> {
          require(currentBaseString != null) { ERROR_MESSAGE_NONBASE_SAME_AS_BASE }
          resultList.add(transformToListElement(currentBaseString, currentBaseString.id)) // same as in base language
        }
        0 -> { // no translation available
          require(currentBaseString != null) { ERROR_MESSAGE_NONBASE_UNTRANSLATED }
          resultList.add(transformToListElement(currentBaseString, null))
        }
        else -> {
          val msgstr = MsgStr(bytes.sliceArray(currentByteIndex until currentByteIndex + stringLength).decodeToString())
          if (currentBaseString == null) {
            resultList.add(transformToListElement(MsgId(msgstr), null))
          } else {
            resultList.add(transformToListElement(currentBaseString, msgstr))
          }
          currentByteIndex += stringLength
        }
      }
    }

    // Plural strings
    while (!isPluralsComplete && (baseStrings == null || resultList.size < baseStrings.size)) {
      val currentBaseString = baseStrings?.get(resultList.size)
      val numGrammaticNumbers = bytes.getOrNull(currentByteIndex)?.toUByte()?.toInt()
      currentByteIndex++
      when (numGrammaticNumbers) {
        // File ended
        null -> isPluralsComplete = true
        0xFF -> // not officially documented, we assume this is not allowed
          throw IllegalArgumentException("Illegal 0xFF byte! The *.lang file format \"only\" supports up to 253 grammatical numbers (255 given).")
        0xFE -> { // special value: translation same as base language
          require(currentBaseString != null) { ERROR_MESSAGE_NONBASE_SAME_AS_BASE }
          resultList.add(transformToListElement(currentBaseString, currentBaseString.id)) // same as in base language
        }
        0 -> { // special value: no translation available
          require (currentBaseString != null) { ERROR_MESSAGE_NONBASE_UNTRANSLATED }
          resultList.add(transformToListElement(currentBaseString, null))
        }
        else -> {
          val grammaticNumbers = mutableListOf<String>()
          for (grammaticNumberIndex in 0 until numGrammaticNumbers) {
            val stringLength = bytes.decodeTwoByteIntAt(currentByteIndex)
            require(stringLength != null) { "File ended unexpectedly. Expected to find msgstr[$grammaticNumberIndex] for message[${resultList.size}] at bytes[$currentByteIndex]!" }
            currentByteIndex += 2
            grammaticNumbers.add(bytes.sliceArray(currentByteIndex until currentByteIndex + stringLength).decodeToString())
            currentByteIndex += stringLength
          }
          if (currentBaseString == null) {
            resultList.add(transformToListElement(MsgId(MsgStr(grammaticNumbers)), null))
          } else {
            resultList.add(transformToListElement(currentBaseString, MsgStr(grammaticNumbers)))
          }
        }
      }
    }

    return resultList
  }

  /**
   * Helper function to decode two bytes as a big endian unsigned number
   * @param index the index of the first of the two bytes in the array
   * @return `null` if [index] is out of bounds, otherwise the big endian value of the two-byte number is returned
   * @throws IllegalArgumentException if [index] is **not** out of bounds, but [index] + 1 **is** out of bounds
   */
  private fun ByteArray.decodeTwoByteIntAt(index: Int): Int? =
    getOrNull(index)?.let { a ->
      getOrNull(index + 1)?.let { b ->
        a.toUByte().toInt().shl(8).or(b.toUByte().toInt())
      } ?: throw IllegalArgumentException("Unexpected end of *.lang file in the middle of a two-byte length value (at index $index)!")
    }

}
