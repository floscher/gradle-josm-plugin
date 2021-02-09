package org.openstreetmap.josm.gradle.plugin.task
import org.gradle.api.tasks.Internal
import org.openstreetmap.josm.gradle.plugin.config.I18nConfig
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet
import org.openstreetmap.josm.gradle.plugin.i18n.io.MoFileDecoder
import javax.inject.Inject
import org.openstreetmap.josm.gradle.plugin.i18n.io.I18nFileDecoder

/**
 * This task "compiles" several *.mo files to *.lang files.
 * For the language specified in [I18nConfig.mainLanguage], only the "msgid" is used (the text which will be translated).
 * For the other languages, the "msgstr" is used (the text which is already translated to this language).
 * @property sourceSet The [I18nSourceSet] for which the *.mo files will be compiled
 */
public open class MoCompile @Inject constructor(
  sourceSet: I18nSourceSet
): CompileToLang(sourceSet, { mo }, "mo") {
  override val decoder: I18nFileDecoder = MoFileDecoder
}
