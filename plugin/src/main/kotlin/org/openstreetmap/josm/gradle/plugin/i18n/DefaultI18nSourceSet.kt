package org.openstreetmap.josm.gradle.plugin.i18n

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet

/**
 * Implementation for a combination of one [SourceDirectorySet]s for each of the three supported translation formats.
 */
class DefaultI18nSourceSet(sourceSet: SourceSet, objFactory: ObjectFactory) : I18nSourceSet {
  override val po: SourceDirectorySet = objFactory.sourceDirectorySet(
    "i18nPo",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.po) source (gettext)")
  override val mo: SourceDirectorySet = objFactory.sourceDirectorySet(
    "i18nMo",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.mo) source (gettext)")
  override val lang: SourceDirectorySet = objFactory.sourceDirectorySet(
    "i18nLang",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.lang) source (custom binary format for JOSM translations)")
  init {
    po.filter{ it.isFile }
    mo.filter{ it.isFile }
    lang.filter{ it.isFile }
    po.include("*.po")
    mo.include("*.mo")
    lang.include("*.lang")
    po.setSrcDirs(listOf("src/${sourceSet.name}/po"))
    mo.setSrcDirs(listOf("src/${sourceSet.name}/mo"))
    lang.setSrcDirs(listOf("src/${sourceSet.name}/lang"))
    sourceSet.allSource.source(po)
    sourceSet.allSource.source(mo)
    sourceSet.allSource.source(lang)
  }

  override val name: String = sourceSet.name

  override fun lang(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(lang)
    return this
  }

  override fun mo(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(mo)
    return this
  }

  override fun po(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(po)
    return this
  }
}

