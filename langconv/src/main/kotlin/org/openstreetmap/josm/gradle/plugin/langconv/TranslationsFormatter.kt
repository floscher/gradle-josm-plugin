package org.openstreetmap.josm.gradle.plugin.langconv

import org.openstreetmap.josm.gradle.plugin.i18n.io.MoWriter
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.i18n.util.formatAsProgressBar

@ExperimentalUnsignedTypes
fun <R : Comparable<R>> Map<String, Map<MsgId, MsgStr>>.getTranslationStatsString(
  baseLangKey: String,
  needsBaseLanguage: Boolean = false,
  ascending: Boolean = false,
  sortBy: (Map.Entry<String, Map<MsgId, MsgStr>>) -> R
): String {

  val baseLang = this[baseLangKey] ?: if (needsBaseLanguage) throw(IllegalArgumentException("No strings in base language 'en' found! Note, that at the moment the base language can't be changed for the 'langconv' program.")) else flatMap { it.value.keys }.map { it to it.id }.toMap()
  val numBaseStrings = baseLang.filter { it.key != MoWriter.EMPTY_MSGID }.size

  val maxKeyLength = keys.map { it.length }.maxOrNull() ?: 0
  val maxStringNumberLength = values.map { it.size.toString().length }.maxOrNull() ?: 0

  return "${"en".padStart(maxKeyLength + 2)}: ${numBaseStrings.toString().padStart(maxStringNumberLength)} strings (base language)\n" +
    entries
      .filter { it.key != "en" }
      .sortedWith( if (ascending) compareBy(sortBy) else compareByDescending(sortBy))
      .joinToString("\n") { stringEntry ->
        val numTranslated = stringEntry.value.keys.filter { it != MoWriter.EMPTY_MSGID }.size

        "${stringEntry.key.padStart(maxKeyLength + 2)}: ${numTranslated.toString().padStart(maxStringNumberLength)} strings ${formatAsProgressBar(numTranslated.toUInt(), numBaseStrings.toUInt())} translated"
      }
}
