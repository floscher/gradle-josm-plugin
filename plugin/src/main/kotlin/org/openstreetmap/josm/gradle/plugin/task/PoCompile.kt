package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.encodeToLangFiles
import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.openstreetmap.josm.gradle.plugin.i18n.io.I18nFileDecoder

/**
 * Compile *.po files (textual gettext files) to *.mo files (binary gettext files).
 *
 * This task requires the command line tool `msgfmt` (part of GNU gettext) to work properly! If the tool is not
 * installed, it will only issue a warning (not fail), but translations from *.po files won't be available.
 *
 * @property sourceSet The source set, for which all *.po files will be compiled to *.mo files.
 */
open class PoCompile @Inject constructor(
  sourceSet: I18nSourceSet
): CompileToLang(sourceSet, { po }, "po") {
  override val decoder: I18nFileDecoder = PoFileDecoder
}

