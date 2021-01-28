package org.openstreetmap.josm.gradle.plugin.i18n.io

public object PoFileDecoder: I18nFileDecoder {
  private val REGEX_MSGCTXT = Regex("^msgctxt \"(.*)\"$")
  private val REGEX_MSGID = Regex("^msgid \"(.*)\"$")
  private val REGEX_MSGID_PLURAL = Regex("^msgid_plural \"(.*)\"$")
  private val REGEX_MSGSTR = Regex("^msgstr \"(.*)\"$")
  private val REGEX_MSGSTR_INDEXED = Regex("^msgstr\\[([0-9]+)] \"(.*)\"$")

  /**
   * Matches every time a line ends with a double quote and the next line starts with a double quote.
   * Every such match will be removed in the process of decoding, so multiline strings appear as oneline strings.
   */
  private val REGEX_MULTILINE_STRING_SEPARATOR = Regex("\"[ \t\r]*\n[ \t\r]*\"")

  override fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr> = decodeToTranslations(bytes.decodeToString())

  public fun decodeToTranslations(poFileContent: String): Map<MsgId, MsgStr> {
    val lines = poFileContent
      .replace(REGEX_MULTILINE_STRING_SEPARATOR, "")
      .lines()
      .map { it.trim() }
      .filter {
        it.isNotBlank() &&
          !it.startsWith("#")
      }

    val result = mutableMapOf<MsgId, MsgStr>()

    var currentLineIndex = 0
    while (currentLineIndex < lines.size) {
      val msgctxt: String? = REGEX_MSGCTXT.matchEntire(lines[currentLineIndex])
        ?.getCaptureGroup(1)
        ?.unescapeCharacters()
        ?.also { currentLineIndex++ } // only advance to next line if there is a plural form

      val msgidSingular: String = requireNotNull(
        REGEX_MSGID.matchEntire(lines[currentLineIndex])
          ?.getCaptureGroup(1)
          ?.unescapeCharacters()
          ?.also { currentLineIndex++ }
      ) { "Syntax error on line `${lines[currentLineIndex]}`: This line was expected to be a `msgid` line" }

      val msgidPlural: String? = REGEX_MSGID_PLURAL.matchEntire(lines[currentLineIndex])
        ?.getCaptureGroup(1)
        ?.unescapeCharacters()
        ?.also { currentLineIndex++ }

      val msgstr: MsgStr = if (msgidPlural != null) {
        val pluralForms = mutableListOf<Pair<Int, String>>()
        do {
          val foundAnotherPluralForm: Boolean = if (currentLineIndex >= lines.size) {
            null // If a translation with plurals is the last one in a file
          } else {
            REGEX_MSGSTR_INDEXED.matchEntire(lines[currentLineIndex])
          }
            ?.let {
              pluralForms.add(it.getCaptureGroup(1).toInt() to it.getCaptureGroup(2).unescapeCharacters())
              true
            }
            ?.also { currentLineIndex++ }
            ?: false
        } while (foundAnotherPluralForm)

        require(pluralForms.isNotEmpty()) {
          "The plural forms for '$msgidSingular' must not be empty!"
        }

        MsgStr(
          pluralForms
            .sortedBy { (foundIndex, _) -> foundIndex }
            .mapIndexed { actualIndex, (foundIndex, string) ->
              require (foundIndex == actualIndex) {
                "Syntax error: The translations for '$msgidSingular' are missing msgstr[$actualIndex] (only found indices ${pluralForms.map { it.first }.sorted().joinToString()})"
              }
              string
            }
        )
      } else {
        MsgStr(requireNotNull(
          REGEX_MSGSTR.matchEntire(lines[currentLineIndex])
            ?.getCaptureGroup(1)
            ?.unescapeCharacters()
            ?.also { currentLineIndex++ }
        ) { "Syntax error on line `${lines[currentLineIndex]}`: This line was expected to be a `msgstr` line" })
      }

      result[MsgId(MsgStr(listOfNotNull(msgidSingular, msgidPlural)), msgctxt)] = msgstr
    }

    return result.toMap()
  }

  private fun MatchResult.getCaptureGroup(i: Int): String = groups[i]?.value
    ?: throw NullPointerException("The RegEx that captured `$value` is missing a capture group with index $i!")


  @OptIn(ExperimentalUnsignedTypes::class)
  private fun String.unescapeCharacters() = this
    .replace("\\\\", "\\")
    .replace("\\r", "\r")
    .replace("\\t", "\t")
    .replace("\\n", "\n")
    .replace("\\a", "\u0007")
    .replace("\\b", "\u0008")
    .replace("\\f", "\u000C")
    .replace("\\v", "\u000B")
    .replace("\\\"", "\"")
    .replace("""\\([0-7]{1,3})""".toRegex()) { // octal escapes
      it.groups[1]!!.value.toUByte(8).toInt().toChar().toString()
    }
    .replace("""\\x([0-9a-f]{1,4})""".toRegex()) { // hexadecimal escapes
      it.groups[1]!!.value.toUInt(16).toInt().toChar().toString()
    }
}
