package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * The content type for gettext files. This is needed for *.po files to signal that the file has UTF-8 charset.
 */
internal val GETTEXT_CONTENT_TYPE_UTF8: String = "Content-Type: text/plain; charset=UTF-8"

/**
 * The empty key. This is used in gettext formats to hold meta information
 */
public val GETTEXT_HEADER_MSGID: MsgId = MsgId(MsgStr(""))
/**
 * An empty header (empty key and empty value).
 * Normally it's better to use [GETTEXT_DEFAULT_HEADER] to avoid encoding issues.
 */
internal val GETTEXT_EMPTY_HEADER: Pair<MsgId, MsgStr> = GETTEXT_HEADER_MSGID to MsgStr("")
/**
 * A basic header for gettext files, which is added when files without metadata are converted.
 */
internal val GETTEXT_DEFAULT_HEADER: Pair<MsgId, MsgStr> = GETTEXT_HEADER_MSGID to MsgStr("$GETTEXT_CONTENT_TYPE_UTF8\n")

/**
 *
 * If the [msgid] is just the empty string, returns a modified [msgstr] that contains the UTF-8 content type.
 * Otherwise returns the given [msgstr] unmodified.
 */
internal fun Map<MsgId, MsgStr>.ensureUtf8EncodingInHeaderEntry(): List<Pair<MsgId, MsgStr>> =
  mapOf(GETTEXT_EMPTY_HEADER).plus(this).map { (key, value) ->
    if (key == GETTEXT_HEADER_MSGID) {
      key to MsgStr(
        value.strings.mapIndexed { index, str ->
          if (index == 0) {
            str.lines()
              .filter { !it.startsWith("Content-Type:") && !it.isEmpty() }
              .plus(GETTEXT_CONTENT_TYPE_UTF8)
              .joinToString("\n", "", "\n")
          } else {
            str
          }
        }
      )
    } else key to value
  }
