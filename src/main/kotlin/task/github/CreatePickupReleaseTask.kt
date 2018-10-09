package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleasesSpec
import org.openstreetmap.josm.gradle.plugin.josm

open class CreatePickupReleaseTask: BaseGithubReleaseTask() {

  @TaskAction
  fun createPickupRelease() {
    val releaseConfigFile = project.extensions.josm.github.releasesConfig
    val releaseConfig = ReleasesSpec.load(releaseConfigFile)

    val release = releaseConfig.pickupRelease
    val client = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.apiUrl)

    client.getReleases().find {it["tag_name"] == release.label}?.let {
      throw GithubReleaseException(
        "Pickup release with label '${release.label}' already exists "
          + "on the GitHub server."
      )
    }

    try {
      client.createRelease(
        tagName = release.label,
        targetCommitish = configuredTargetCommitish,
        name = release.name,
        body = release.defaultDescriptionForPickupRelease())
      logger.lifecycle("Pickup release '{}' created in GitHub repository",
        release.label)
    } catch(e: Throwable) {
      throw GithubReleaseException(e)
    }
  }
}
