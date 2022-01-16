package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.SetProperty

class GitlabConfig(project: Project) {
  /**
   * The name of the publication that should be attached to the release.
   * In version `0.8.0` this was changed from a [Set<String>] value to a [SetProperty<String>]
   *
   * @since 0.6.3
   */
  val publicationNames: SetProperty<String> =  project.objects.setProperty(String::class.java).convention(
    project.extensions.getByType(BasePluginExtension::class.java).archivesName
      .map { setOf("org/openstreetmap/josm/plugins/$it") }
  )

  /**
   * Adds an additional publication name to be released with task `releaseToGitlab` to GitLab
   * @since 0.6.3
   */
  fun publicationName(name: String) = publicationNames.add(name)
}
