package org.openstreetmap.josm.gradle.plugin.i18n

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet

/**
 * Interface that defines a container for three [SourceDirectorySet]s each one containing files for one of these
 * translation formats: *.po, *.mo (both from gettext) and *.lang (custom format for JOSM).
 */
interface I18nSourceSet {
  /**
   * Source set for *.po files (by default only files ending in `.po` are considered).
   */
  val po: SourceDirectorySet
  /**
   * Source set for *.mo files (by default only files ending in `.mo` are considered).
   */
  val mo: SourceDirectorySet
  /**
   * Source set for *.lang files (by default only files ending in `.lang` are considered)
   */
  val lang: SourceDirectorySet

  /**
   * The name of the [SourceSet] for which this interface provides [SourceDirectorySet]s for i18n.
   */
  val name: String

  /**
   * Configure the [SourceDirectorySet] for *.lang files using an [Action].
   */
  fun lang(configureAction: Action<in SourceDirectorySet>): I18nSourceSet

  /**
   * Configure the [SourceDirectorySet] for *.mo files using an [Action].
   */
  fun mo(configureAction: Action<in SourceDirectorySet>): I18nSourceSet

  /**
   * Configure the [SourceDirectorySet] for *.po files using an [Action].
   */
  fun po(configureAction: Action<in SourceDirectorySet>): I18nSourceSet
}
