package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_API_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_UPLOAD_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_URL
import org.openstreetmap.josm.gradle.plugin.task.GithubReleaseTaskException
import java.io.File
import java.io.IOException

/**
 * Configuration options for the new GitHub releases feature.
 *
 * Note: This is currently in beta stage, so expect sudden changes to this API anytime.
 */
class GithubConfig(project: Project) {

  private fun Project.findNonBlankProperty(name: String) = findProperty(name)?.toString()?.takeIf { it.isNotBlank() }

  private val repositoryOwnerProperty = "josm.github.repositoryOwner"
  private val repositoryOwnerEnv = "GITHUB_REPOSITORY_OWNER"
  /**
   * The GitHub account that owns the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryOwner: String? = project.findNonBlankProperty(repositoryOwnerProperty) ?: System.getenv(repositoryOwnerEnv)
    get() = field ?: throw unsetFieldException("repositoryOwner", "GitHub repository owner (user or org)", repositoryOwnerProperty, repositoryOwnerEnv)

  private val repositoryProperty = "josm.github.repositoryName"
  private val repositoryEnv = "GITHUB_REPOSITORY_NAME"
  /**
   * The name of the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryName: String? = project.findNonBlankProperty(repositoryProperty) ?: System.getenv(repositoryEnv)
    get() = field ?: throw unsetFieldException("repositoryName", "GitHub repository name", repositoryProperty, repositoryEnv)

  private val accessTokenProperty = "josm.github.accessToken"
  private val accessTokenEnv = "GITHUB_ACCESS_TOKEN"
  /**
   * The access token that will be used for authentication when uploading the release.
   *
   * @since 0.5.3
   */
  var accessToken: String? = project.findNonBlankProperty(accessTokenProperty) ?: System.getenv(accessTokenEnv)
    get() = field ?: throw unsetFieldException("accessToken", "GitHub access token", accessTokenProperty, accessTokenEnv)

  /**
   * The base API URL for the Github releases API.
   * Defaults to `https://api.github.com`.
   *
   * @since 0.5.3
   */
  var apiUrl: String = System.getenv("GITHUB_API_URL") ?: DEFAULT_GITHUB_API_URL

  /**
   * The base API URL to upload release assets.
   * Defaults to `https://uploads.github.com`.
   *
   * @since 0.5.3
   */
  var uploadUrl: String = System.getenv("GITHUB_UPLOAD_URL") ?: DEFAULT_GITHUB_UPLOAD_URL

  /**
   * The base GitHub URL.
   * Defaults to `https://github.com`
   *
   * @since 0.5.3
   */
  var mainUrl: String = System.getenv("GITHUB_MAIN_URL") ?: DEFAULT_GITHUB_URL

  /**
   * The list of releases in YAML format. The file must exist and be readable,
   * otherwise an exception is thrown on first access of this property.
   * Defaults to the file `releases.yml` in [Project.getProjectDir].
   *
   * @since 0.5.3
   */
  var releasesConfig: File = File(project.projectDir, "releases.yml")
    get() = if (!field.isFile || !field.exists() || !field.canRead()) {
      throw IOException("Releases configuration file '${field.absolutePath}' doesn't exist or can't be read!")
    } else {
      field
    }

  /**
   * Specifies the commitish value that determines where the Git tag is created from.
   * Can be any branch or commit SHA. Defaults to `master`.
   *
   * @since 0.5.3
   */
  var targetCommitish: String = "master"

  /**
   * @param [tagLabel] the label of the tag for which the URL is returned
   * @return the full URL to a specific tag in the webinterface of GitHub
   * @since 0.5.3
   */
  fun getReleaseUrl(tagLabel: String) = "$mainUrl/$repositoryOwner/$repositoryName/releases/tag/$tagLabel"

  private fun unsetFieldException(fieldName: String, fieldDescription: String, property: String? = null, env: String? = null) =
    GithubReleaseTaskException(
      "No $fieldDescription configured!\n  Configure it by adding:\n  * " +
        listOfNotNull(
          "a value for project.josm.github.$fieldName to your Gradle build script",
          if (property != null) "a project property $property" else null,
          if (env != null) "an environment variable $env" else null
        ).joinToString("\n  * ")
    )
}
