package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A translatable string ([MsgStr]) in the base language with an optional [String] context.
 * @param id the string (optonally with plural versions) to be translated
 * @param context an optional context with additional information for which situations the string should be translated
 *   (e.g. for disambiguation of multiple identicals strings that should be translated differently in different situations)
 */
public data class MsgId(val id: MsgStr, val context: String? = null): Comparable<MsgId> {
  override fun compareTo(other: MsgId): Int =
    // first sort IDs with fewer grammatical forms first
    id.strings.size.compareTo(other.id.strings.size).takeIf { it != 0 }
    // fallback to comparing contexts
    ?: when {
      // If my context is null, while the other's context is non-null: I go first. Otherwise fallback to next check.
      context == null -> other.context?.let { -1 }
      // If my context is non-null, while the other's context is null: The other one goes first
      other.context == null -> 1
      // If both contexts are non-null: If they are different, use the order of the contexts. Otherwise fallback to next check.
      else -> context.compareTo(other.context).takeIf { it != 0 }
    }
    // Go through all the strings and compare to the corresponding string of the other object. Return the comparison result of the first difference.
    ?: id.strings.zip(other.id.strings).map { (myString, otherString) -> myString.compareTo(otherString) }.firstOrNull { it != 0 }
    // At this point the objects are identical (same number of strings, same context and all strings are the same)
    ?: 0
}
