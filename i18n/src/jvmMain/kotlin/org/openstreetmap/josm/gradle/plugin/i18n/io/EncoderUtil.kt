package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.File
import java.io.IOException

/**
 * Takes translation definitions in the form of [MsgId]s and [MsgStr]s for multiple languages.
 * These are then written to a directory in the *.lang file format.
 * @param [outputDir] the directory into which the *.lang files should be written
 * @param [translations] a map with language codes as keys and maps as values.
 *   These maps associate each [MsgId] with a [MsgStr], which is the translation of the [MsgId].
 * @param [baseLanguage] the language code of the language in which the strings were written in the source code.
 * @throws IOException if writing the file is not successful
 * @throws IllegalArgumentException if the given translations can't be represented as a *.lang file (too long strings, too many grammatical numbers, …)
 * @see encodeToLangBytes
 */
public fun encodeToLangFiles(
  translations: Map<String, Map<MsgId, MsgStr>>,
  outputDir: File,
  baseLanguage: String = LangFileEncoder.DEFAULT_BASE_LANGUAGE
): Set<File> =
  I18nTranslationData(
    baseLanguage,
    translations[baseLanguage]?.map { it.key } ?: throw IOException("No messages for the base language found!"),
    translations.minus(baseLanguage)
  )
    .encodeToMultipleLangFiles()
    .map { (language, bytes) -> File(outputDir, "$language.lang").apply { writeBytes(bytes) } }
    .toSet()
