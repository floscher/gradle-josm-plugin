package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import java.io.File
import java.io.IOException

private const val EMPTY_STRING = ""

// the default API URL for the GitHub API
private const val DEFAULT_API_URL = "https://api.github.com"
// the default upload URL to upload a release asset
private const val DEFAULT_UPLOAD_URL = "https://uploads.github.com"
// the default base URL to access a release
private const val DEFAULT_MAIN_URL = "https://github.com"

private const val PROPERTY_REPOSITORY_OWNER = "josm.github.repositoryOwner"
private const val ENV_REPOSITORY_OWNER = "GITHUB_REPOSITORY_OWNER"

private const val PROPERTY_REPOSITORY_NAME = "josm.github.repositoryName"
private const val ENV_REPOSITORY_NAME = "GITHUB_REPOSITORY_NAME"

private const val PROPERTY_ACCESS_TOKEN = "josm.github.accessToken"
private const val ENV_ACCESS_TOKEN = "GITHUB_ACCESS_TOKEN"

private const val ENV_API_URL = "GITHUB_API_URL"
private const val ENV_UPLOAD_URL = "GITHUB_UPLOAD_URL"
private const val ENV_MAIN_URL = "GITHUB_MAIN_URL"

/**
 * Configuration options for the new GitHub releases feature.
 *
 * Note: This is currently in beta stage, so expect sudden changes to this API anytime.
 */
class GithubConfig(project: Project) {

  private fun Project.findNonBlankProperty(name: String) = findProperty(name)?.toString()?.takeIf { it.isNotBlank() }

  /**
   * The GitHub account that owns the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryOwner: String = project.findNonBlankProperty(PROPERTY_REPOSITORY_OWNER) ?: System.getenv(ENV_REPOSITORY_OWNER) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("repositoryOwner", "GitHub repository owner (user or org)", PROPERTY_REPOSITORY_OWNER, ENV_REPOSITORY_OWNER)

  /**
   * The name of the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryName: String = project.findNonBlankProperty(PROPERTY_REPOSITORY_NAME) ?: System.getenv(ENV_REPOSITORY_NAME) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("repositoryName", "GitHub repository name", PROPERTY_REPOSITORY_NAME, ENV_REPOSITORY_NAME)

  /**
   * The access token that will be used for authentication when uploading the release.
   *
   * @since 0.5.3
   */
  var accessToken: String = project.findNonBlankProperty(PROPERTY_ACCESS_TOKEN) ?: System.getenv(ENV_ACCESS_TOKEN) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("accessToken", "GitHub access token", PROPERTY_ACCESS_TOKEN, ENV_ACCESS_TOKEN)

  /**
   * The base API URL for the Github releases API.
   * Defaults to `https://api.github.com`.
   *
   * @since 0.5.3
   */
  var apiUrl: String = System.getenv(ENV_API_URL) ?: DEFAULT_API_URL

  /**
   * The base API URL to upload release assets.
   * Defaults to `https://uploads.github.com`.
   *
   * @since 0.5.3
   */
  var uploadUrl: String = System.getenv(ENV_UPLOAD_URL) ?: DEFAULT_UPLOAD_URL

  /**
   * The base GitHub URL.
   * Defaults to `https://github.com`
   *
   * @since 0.5.3
   */
  var mainUrl: String = System.getenv(ENV_MAIN_URL) ?: DEFAULT_MAIN_URL

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
    GithubReleaseException(
      "No $fieldDescription configured!\n  Configure it by adding:\n  * " +
        listOfNotNull(
          "a value for project.josm.github.$fieldName to your Gradle build script",
          if (property != null) "a project property $property" else null,
          if (env != null) "an environment variable $env" else null
        ).joinToString("\n  * ")
    )
}
