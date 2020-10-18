package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * The *.po file format as described in [https://www.gnu.org/software/gettext/manual/html_node/PO-Files.html].
 *
 * This implementation completely disregards metadata
 */
public class PoFormat: I18nFileFormat {
  private companion object {
    val REGEX_MSGCTXT = "^msgctxt \"(.*)\"$".toRegex()
    val REGEX_MSGID = "^msgid \"(.*)\"$".toRegex()
    val REGEX_MSGID_PLURAL = "^msgid_plural \"(.*)\"$".toRegex()
    val REGEX_MSGSTR = "^msgstr \"(.*)\"$".toRegex()
    val REGEX_MSGSTR_INDEXED = "^msgstr\\[([0-9]+)] \"(.*)\"$".toRegex()
  }

  /**
   * Checks the translations for common mistakes, can be used to get warnings before encoding translations
   * @return warning messages for the given translations, empty if there are none
   */
  public fun checkTranslations(translations: Map<MsgId, MsgStr>): List<String> =
    mapOf('\u0007' to "\\a", '\b' to "\\b", '\u000C' to "\\f", '\r' to "\\r")
    .mapNotNull { (char, charLabel) ->
      translations.map { it.key }
        .filter { it.id.strings.any { s -> s.contains(char) } }
        .count()
        .takeIf { it >= 1 }
        ?.let { "Internationalized messages should not contain the '${charLabel}' escape sequence! ($it of them do contain it)" }
    }

  override fun encodeToByteArray(translations: Map<MsgId, MsgStr>): ByteArray =
    translations
      .ensureUtf8EncodingInHeaderEntry()
      .joinToString("\n\n") { (msgid, msgstr) ->
        listOfNotNull(
          msgid.context?.let { """msgctxt "${it.escapeCharacters()}"""" },
          encodeMsgIdLines(msgid.id.strings),
          encodeMsgStrLines(msgstr.strings, msgid.id.strings.size >= 2)
        ).joinToString("\n")
      }
      .encodeToByteArray()

  private fun encodeMsgIdLines(msgids: List<String>): String = when (msgids.size) {
    1, 2 -> listOfNotNull(
      """msgid "${msgids.first().escapeCharacters()}"""",
      if (msgids.size >= 2) """msgid_plural "${msgids[1].escapeCharacters()}"""" else null
    ).joinToString("\n")
    else -> throw IllegalArgumentException("Only one or two MsgIds are allowed (found ${msgids.size})!")
  }

  private fun encodeMsgStrLines(msgstrs: List<String>, hasPlurals: Boolean) = if (hasPlurals) {
    msgstrs.mapIndexed { i, str -> """msgstr[$i] "${str.escapeCharacters()}"""" }.joinToString("\n")
  } else {
    """msgstr "${msgstrs.first().escapeCharacters()}""""
  }

  override fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr> = decodeToTranslations(bytes.decodeToString())

  public fun decodeToTranslations(poFileContent: String): Map<MsgId, MsgStr> {
    val lines = poFileContent
      .replace("\"\\s*\n\\s*\"", "")
      .lines()
      .mapIndexed { lineIndex, line -> lineIndex to line.trim() }
      .filter { (_, line) ->
        line.isNotBlank() &&
        !line.startsWith("#")
      }

    val result = mutableMapOf<MsgId, MsgStr>()

    var currentLineIndex = 0
    while (currentLineIndex < lines.size) {
      val msgctxt: String? = REGEX_MSGCTXT.matchEntire(lines[currentLineIndex].second)
        ?.getCaptureGroup(1)
        ?.unescapeCharacters()
        ?.also { currentLineIndex++ } // only advance to next line if there is a plural form

      val msgidSingular: String =
        REGEX_MSGID.matchEntire(lines[currentLineIndex].second)
          ?.getCaptureGroup(1)
          ?.unescapeCharacters()
          ?.also { currentLineIndex++ }
          ?: throw IllegalArgumentException("Syntax error on line ${lines[currentLineIndex].first}: This line was expected to be a `msgid` line (${lines[currentLineIndex].second})")

      val msgidPlural: String? = REGEX_MSGID_PLURAL.matchEntire(lines[currentLineIndex].second)
        ?.getCaptureGroup(1)
        ?.unescapeCharacters()
        ?.also { currentLineIndex++ }

      val msgstr: MsgStr = if (msgidPlural != null) {
        val pluralForms = mutableListOf<Pair<Int, String>>()
        do {
          val foundAnotherPluralForm: Boolean = if (currentLineIndex >= lines.size) {
            null // If a translation with plurals is the last one in a file
          } else {
            REGEX_MSGSTR_INDEXED.matchEntire(lines[currentLineIndex].second)
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
        MsgStr(
          REGEX_MSGSTR.matchEntire(lines[currentLineIndex].second)
            ?.getCaptureGroup(1)
            ?.unescapeCharacters()
            ?.also { currentLineIndex++ }
            ?: throw IllegalArgumentException("Syntax error on line ${lines[currentLineIndex].first}: This line was expected to be a `msgstr` line")
        )
      }

      result[MsgId(MsgStr(listOfNotNull(msgidSingular, msgidPlural)), msgctxt)] = msgstr
    }

    return result.toMap()
  }

  private fun MatchResult.getCaptureGroup(i: Int): String = groups[i]?.value
    ?: throw NullPointerException("The RegEx that captured `$value` is missing a capture group with index $i!")

  /**
   * Escapes the strings in *.po files.
   *
   * According to [the PO file documentation](https://www.gnu.org/software/gettext/manual/html_node/PO-Files.html)
   * strings are escaped in C style.
   *
   * To ensure compatibility with the `msgfmt` gettext utility, these characters are not escaped:
   * * `?` (in C it is escaped to avoid trigraphs)
   * * `'`
   * * ASCII character represented by byte `0x1B` (Escape character)
   * * Escapes for unicode characters (`\uhhhh` or `\Uhhhhhhhh` where `h` is a hexadecimal digit)
   *   also aren't recognized by `msgfmt`
   */
  private fun String.escapeCharacters() = this
    .replace("\\", "\\\\") // this has to be the first replacement to avoid double escaping
    .replace("\r", "\\r")
    .replace("\t", "\\t")
    .replace("\n", "\\n")
    .replace("\u0007", "\\a")
    .replace("\u0008", "\\b")
    .replace("\u000C", "\\f")
    .replace("\u000B", "\\v")
    .replace("\"", "\\\"")


  @OptIn(ExperimentalUnsignedTypes::class)
  private fun String.unescapeCharacters() = this
    .replace("\\\\", "\\") // this has to be the first replacement to avoid double escaping
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
