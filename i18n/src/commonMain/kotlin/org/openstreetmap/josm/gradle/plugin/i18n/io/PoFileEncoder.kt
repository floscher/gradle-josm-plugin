package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * The *.po file format as described in [https://www.gnu.org/software/gettext/manual/html_node/PO-Files.html].
 *
 * This implementation completely disregards metadata
 */
public object PoFileEncoder: I18nFileEncoder {

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
      .sortedBy { it.first }
      .joinToString("\n\n", "", "\n") { (msgid, msgstr) ->
        listOfNotNull(
          msgid.context?.let { """msgctxt "${it.escapeCharacters()}"""" },
          encodeMsgIdLines(msgid.id.strings),
          encodeMsgStrLines(msgstr.strings, msgid.id.strings.size >= 2)
        ).joinToString("\n")
      }
      .encodeToByteArray()

  private fun encodeMsgIdLines(msgids: List<String>): String = when (msgids.size) {
    1, 2 -> listOfNotNull(
      """msgid "${msgids.first().escapeCharacters().toMultiline()}"""",
      if (msgids.size >= 2) """msgid_plural "${msgids[1].escapeCharacters().toMultiline()}"""" else null
    ).joinToString("\n")
    else -> throw IllegalArgumentException("Only one or two MsgIds are allowed in PO files (found ${msgids.size})!")
  }

  private fun encodeMsgStrLines(msgstrs: List<String>, hasPlurals: Boolean) = if (hasPlurals) {
    msgstrs.mapIndexed { i, str -> """msgstr[$i] "${str.escapeCharacters().toMultiline()}"""" }.joinToString("\n")
  } else {
    """msgstr "${msgstrs.first().escapeCharacters().toMultiline()}""""
  }

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

  /**
   * This should be applied to the strings in `msgid "(.*)"`, `msgstr "(.*)"` and similar.
   * If these strings contain a newline `\n`, the string is distributed over multiple lines.
   * Also if it is such a multiline string, the first line is just the empty string.
   */
  private fun String.toMultiline() = if (contains("\\n")) {
    "\"\n  \"" + replace("\\n", "\\n\"\n  \"")
  } else this
}
