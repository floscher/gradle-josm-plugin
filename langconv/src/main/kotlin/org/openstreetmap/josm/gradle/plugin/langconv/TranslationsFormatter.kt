package org.openstreetmap.josm.gradle.plugin.langconv

import org.openstreetmap.josm.gradle.plugin.common.formatAsProgressBar
import org.openstreetmap.josm.gradle.plugin.i18n.io.GETTEXT_HEADER_MSGID
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr

@OptIn(ExperimentalUnsignedTypes::class)
fun <R : Comparable<R>> Map<String, Map<MsgId, MsgStr>>.getTranslationStatsString(
  baseLangKey: String,
  needsBaseLanguage: Boolean = false,
  ascending: Boolean = false,
  sortBy: (Map.Entry<String, Map<MsgId, MsgStr>>) -> R
): String {

  val baseLang = this[baseLangKey] ?: if (needsBaseLanguage) throw(IllegalArgumentException("No strings in base language 'en' found! Note, that at the moment the base language can't be changed for the 'langconv' program.")) else flatMap { it.value.keys }.map { it to it.id }.toMap()
  val numBaseStrings = baseLang.filter { it.key != GETTEXT_HEADER_MSGID }.size

  val maxKeyLength = keys.map { it.length }.maxOrNull() ?: 0
  val maxStringNumberLength = values.map { it.size.toString().length }.maxOrNull() ?: 0

  return "${"en".padStart(maxKeyLength + 2)}: ${numBaseStrings.toString().padStart(maxStringNumberLength)} strings (base language)\n" +
    entries
      .filter { it.key != "en" }
      .sortedWith( if (ascending) compareBy(sortBy) else compareByDescending(sortBy))
      .joinToString("\n") { stringEntry ->
        val numTranslated = stringEntry.value.keys.filter { it != GETTEXT_HEADER_MSGID }.size

        "${stringEntry.key.padStart(maxKeyLength + 2)}: ${numTranslated.toString().padStart(maxStringNumberLength)} strings ${formatAsProgressBar(numTranslated.toUInt(), numBaseStrings.toUInt())} translated"
      }
}
