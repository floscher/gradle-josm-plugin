package org.openstreetmap.josm.gradle.plugin.i18n

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

class DefaultI18nSourceSet(sourceSet: SourceSet, sourceDirectorySetFactory: SourceDirectorySetFactory) : I18nSourceSet {
  override val po: SourceDirectorySet = sourceDirectorySetFactory.create(
    "i18nPo",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.po) source (gettext)")
  override val mo: SourceDirectorySet = sourceDirectorySetFactory.create(
    "i18nMo",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.mo) source (gettext)")
  override val lang: SourceDirectorySet = sourceDirectorySetFactory.create(
    "i18nLang",
    (sourceSet as? DefaultSourceSet)?.displayName ?: sourceSet.name  + " I18n (*.lang) source (custom binary format for JOSM translations)")
  init {
    po.filter{ it.isFile }
    mo.filter{ it.isFile }
    lang.filter{ it.isFile }
    po.include("**/*.po")
    mo.include("**/*.mo")
    lang.include("**/*.lang")
    po.setSrcDirs(listOf("src/${sourceSet.name}/po"))
    mo.setSrcDirs(listOf("src/${sourceSet.name}/mo"))
    lang.setSrcDirs(listOf("src/${sourceSet.name}/lang"))
    sourceSet.allSource.source(po)
    sourceSet.allSource.source(mo)
    sourceSet.allSource.source(lang)
  }

  override val name = sourceSet.name

  override fun lang(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet {
    ConfigureUtil.configure(configureClosure, lang)
    return this
  }

  override fun lang(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(lang)
    return this
  }

  override fun mo(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet {
    ConfigureUtil.configure(configureClosure, mo)
    return this
  }

  override fun mo(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(mo)
    return this
  }
  override fun po(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet {
    ConfigureUtil.configure(configureClosure, po)
    return this
  }

  override fun po(configureAction: Action<in SourceDirectorySet>): I18nSourceSet {
    configureAction.execute(po)
    return this
  }
}

