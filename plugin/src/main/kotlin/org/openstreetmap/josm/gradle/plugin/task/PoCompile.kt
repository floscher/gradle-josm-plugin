package org.openstreetmap.josm.gradle.plugin.task

import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.I18nFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFileDecoder
import javax.inject.Inject

/**
 * Compile *.po files (textual gettext files) to *.lang files (custom binary files for JOSM).
 *
 * @property sourceSet The source set, for which all *.po files will be compiled to *.lang files.
 */
open class PoCompile @Inject constructor(
  sourceSet: I18nSourceSet
): CompileToLang(sourceSet, { po }, "po") {
  override val decoder: I18nFileDecoder = PoFileDecoder
}

