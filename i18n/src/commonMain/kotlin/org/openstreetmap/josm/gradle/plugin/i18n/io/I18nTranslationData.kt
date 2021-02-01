package org.openstreetmap.josm.gradle.plugin.i18n.io

public data class I18nTranslationData(
  val baseLanguage: String,
  val baseMessages: List<MsgId>,
  val translatedMessages: Map<String, Map<MsgId, MsgStr>>
) {
  init {
    require(!translatedMessages.containsKey(baseLanguage))
    require(translatedMessages.flatMap { it.value.keys }.all { baseMessages.contains(it) })
  }

  /**
   * Takes translations for multiple languages and converts them into the *.lang file format used by JOSM.
   */
  public fun encodeToMultipleLangFiles(): Map<String, ByteArray> =
    LangFileEncoder(baseMessages).let { encoder ->
      translatedMessages.entries
        .associate { (key, value) ->
          key to encoder.encodeToByteArray(value)
        }
        .plus(baseLanguage to encoder.encodeToBaseLanguageByteArray())
    }
}
