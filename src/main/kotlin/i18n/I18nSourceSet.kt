package org.openstreetmap.josm.gradle.plugin.i18n

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

interface I18nSourceSet {
  val po: SourceDirectorySet
  val mo: SourceDirectorySet
  val lang: SourceDirectorySet

  val name: String
  fun lang(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet
  fun lang(configureAction: Action<in SourceDirectorySet>): I18nSourceSet
  fun mo(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet
  fun mo(configureAction: Action<in SourceDirectorySet>): I18nSourceSet
  fun po(configureClosure: Closure<in SourceDirectorySet>): I18nSourceSet
  fun po(configureAction: Action<in SourceDirectorySet>): I18nSourceSet
}
