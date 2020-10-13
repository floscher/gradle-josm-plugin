package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention

class GitlabConfig(project: Project) {
  /**
   * The name of the publication that should be attached to the release.
   *
   * @since 0.6.3
   */
  val publicationNames: MutableSet<String> = mutableSetOf("org/openstreetmap/josm/plugins/${project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName}")

  /**
   * Adds an additional publication name to be released with task `releaseToGitlab` to GitLab
   * @since 0.6.3
   */
  fun publicationName(name: String) = publicationNames.add(name)
}
