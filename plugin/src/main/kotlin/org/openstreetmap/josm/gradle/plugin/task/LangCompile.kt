package org.openstreetmap.josm.gradle.plugin.task

import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.I18nFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangFileEncoder
import java.io.File
import javax.inject.Inject

/**
 * This "compiles" the *.lang files in [sourceSet] into `$buildDir/i18n/${sourceSet.name}/lang/data`.
 *
 * In the process they are decoded using [LangFileDecoder] and re-encoded using [LangFileEncoder] to ensure they are
 * valid *.lang files and in order to show statistics on the command line.
 *
 * @property sourceSet The source set from which all *.lang files are put into the destination
 */
open class LangCompile @Inject constructor(sourceSet: I18nSourceSet): CompileToLang(sourceSet, { lang }, "lang") {

  override val decoder: I18nFileDecoder by lazy {
    this.extractSources(sourceSet).srcDirs
      .flatMap { project.fileTree(it).files }
      .singleOrNull(this::filterIsExcludedBaseFile)
      ?.let { LangFileDecoder(it.readBytes()) }
      ?: throw IllegalArgumentException("No base language file `$baseLanguage.lang` found, or more than one with this same name.")
  }

  override fun filterIsExcludedBaseFile(file: File): Boolean = file.name == "${baseLanguage.get()}.$fileExtension"
}
