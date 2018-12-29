package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A translatable string ([MsgStr]) in the base language with an optional [String] context.
 * @param id the string (optonally with plural versions) to be translated
 * @param context an optional context with additional information for which situations the string should be translated
 *   (e.g. for disambiguation of multiple identicals strings that should be translated differently in different situations)
 */
data class MsgId(val id: MsgStr, val context: String? = null) {
  companion object {
    /**
     * The `0x04` character that is used by default to separate the [context] from the [id]
     * in the [ByteArray] representation of this object.
     */
    const val CONTEXT_SEPARATOR = '\u0004'
  }

  /**
   * Converts this object to the default [ByteArray] representation as used in *.mo files.
   * The [id] is converted according to [MsgStr.toByteArray], it is separated from the context by [CONTEXT_SEPARATOR].
   */
  fun toByteArray(): ByteArray =
    String(id.toByteArray())
      .let { if (context == null) it else "$context$CONTEXT_SEPARATOR$it" }
      .toByteArray(Charsets.UTF_8)
}

/**
 * Returns a MsgId for a string as it is saved in a *.mo file (context and EOT byte, then the string)
 */
internal fun ByteArray.toMsgId(): MsgId {
  val string = this.toString(Charsets.UTF_8)
  val csIndex = string.indexOf(MsgId.CONTEXT_SEPARATOR)
  return if (csIndex >= 0) {
    MsgId(
      MsgStr(string.substring(csIndex + 1).split(MsgStr.GRAMMATICAL_NUMBER_SEPARATOR)),
      string.substring(0, csIndex)
    )
  } else {
    MsgId(MsgStr(string.split(MsgStr.GRAMMATICAL_NUMBER_SEPARATOR)))
  }
}
