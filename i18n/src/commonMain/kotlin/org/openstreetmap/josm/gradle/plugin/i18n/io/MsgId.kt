package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * A translatable string ([MsgStr]) in the base language with an optional [String] context.
 * @param id the string (optonally with plural versions) to be translated
 * @param context an optional context with additional information for which situations the string should be translated
 *   (e.g. for disambiguation of multiple identicals strings that should be translated differently in different situations)
 */
public data class MsgId(val id: MsgStr, val context: String? = null)
