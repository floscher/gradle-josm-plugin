package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A single translatable string in singular and optionally one or more plural versions.
 * @param strings a list of all singular and plural variants of this string. The first element is the singular version,
 *   the rest of the elements are the plurals.
 */
data class MsgStr(val strings: List<String>) {
  companion object {
    /**
     * The `0x00` character, that is used by default to separate the strings in a [MsgStr]
     * in the [ByteArray] representation of the object.
     */
    const val GRAMMATICAL_NUMBER_SEPARATOR = '\u0000'
  }

  /**
   * @param singularString the singular version of the translatable string
   * @param pluralStrings the plural versions of the translatable string
   */
  constructor(singularString: String, vararg pluralStrings: String): this(listOf(singularString, *pluralStrings))
  init {
    require(strings.isNotEmpty()){"A MsgStr has to consist of at least one string!"}
  }

  /**
   * Converts this object to the default [ByteArray] representation as used in *.mo files.
   * The strings for the different grammatical numbers (singular, plural(s)) are separated by [GRAMMATICAL_NUMBER_SEPARATOR].
   * @returns this [MsgStr] represented as a [ByteArray] (strings separated by [GRAMMATICAL_NUMBER_SEPARATOR])
   */
  fun toByteArray(): ByteArray =
    strings.joinToString(GRAMMATICAL_NUMBER_SEPARATOR.toString()).encodeToByteArray()
}
