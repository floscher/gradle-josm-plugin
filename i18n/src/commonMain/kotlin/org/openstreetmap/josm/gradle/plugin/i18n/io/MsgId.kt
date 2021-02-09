package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A translatable string ([MsgStr]) in the base language with an optional [String] context.
 * @param id the string (optonally with plural versions) to be translated
 * @param context an optional context with additional information for which situations the string should be translated
 *   (e.g. for disambiguation of multiple identicals strings that should be translated differently in different situations)
 */
public data class MsgId(val id: MsgStr, val context: String? = null): Comparable<MsgId> {
  override fun compareTo(other: MsgId): Int {
    val grammaticalFormsComparison = id.strings.size.compareTo(other.id.strings.size)
    if (grammaticalFormsComparison != 0) {
      return grammaticalFormsComparison
    }

    if (context == null && other.context != null) {
      return -1
    } else if (context != null && other.context == null) {
      return 1
    } else if (context != null && other.context != null) {
      context.compareTo(other.context).takeIf { it != 0 }?.apply { return this }
    }
    return id.strings.zip(other.id.strings).map { (myString, otherString) -> myString.compareTo(otherString) }.firstOrNull { it != 0 } ?: 0
  }
}
