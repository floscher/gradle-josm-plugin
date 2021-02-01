package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A single translatable string in singular and optionally one or more plural versions.
 * @param strings a list of all singular and plural variants of this string. The first element is the singular version,
 *   the rest of the elements are the plurals.
 */
public data class MsgStr(val strings: List<String>) {

  /**
   * @param singularString the singular version of the translatable string
   * @param pluralStrings the plural versions of the translatable string
   */
  public constructor(singularString: String, vararg pluralStrings: String): this(listOf(singularString).plus(pluralStrings))
  init {
    require(strings.isNotEmpty()){"A MsgStr has to consist of at least one string!"}
  }
}
