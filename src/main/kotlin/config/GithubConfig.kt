package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import java.io.File
import java.io.IOException
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * Configuration options for the new GitHub releases feature.
 *
 * Note: This is currently in beta stage, so expect sudden changes to this API anytime.
 */
class GithubConfig(project: Project) {

  companion object {
    private const val EMPTY_STRING = ""

    // the default API URL for the GitHub API
    private const val DEFAULT_API_URL = "https://api.github.com"
    // the default upload URL to upload a release asset
    private const val DEFAULT_UPLOAD_URL = "https://uploads.github.com"

    private const val ENV_ACCESS_TOKEN = "GITHUB_ACCESS_TOKEN"
    private const val ENV_API_URL = "GITHUB_API_URL"
    private const val ENV_REPOSITORY_NAME = "GITHUB_REPOSITORY_NAME"
    private const val ENV_REPOSITORY_OWNER = "GITHUB_REPOSITORY_OWNER"
    private const val ENV_UPLOAD_URL = "GITHUB_UPLOAD_URL"

    const val PROPERTY_ACCESS_TOKEN = "josm.github.accessToken"

    const val DEFAULT_TARGET_COMMITTISH = "master"
  }

  /**
   * The GitHub account that owns the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryOwner: String = System.getenv(ENV_REPOSITORY_OWNER) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("repositoryOwner", "GitHub repository owner (user or org)", ENV_REPOSITORY_OWNER)

  /**
   * The name of the repository to which releases will be published.
   *
   * @since 0.5.3
   */
  var repositoryName: String = System.getenv(ENV_REPOSITORY_NAME) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("repositoryName", "GitHub repository name", ENV_REPOSITORY_NAME)

  /**
   * The access token that will be used for authentication when uploading the release.
   *
   * This property is readonly, because the token should not be stored in the `build.gradle` file.
   * Set this property by providing the environment variable [ENV_ACCESS_TOKEN] or setting the property
   * [PROPERTY_ACCESS_TOKEN] in `~/.gradle/gradle.properties`!
   * Do **not** store it in a file that is committed to version control!
   *
   * @since 0.5.3
   */
  val accessToken: String = project.findProperty(PROPERTY_ACCESS_TOKEN)?.toString()?.takeIf { it.isNotBlank() } ?: System.getenv(ENV_ACCESS_TOKEN) ?: EMPTY_STRING
    get() = field.takeIf { it.isNotBlank() } ?: throw unsetFieldException("accessToken", "GitHub access token", ENV_ACCESS_TOKEN, PROPERTY_ACCESS_TOKEN)

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
  var targetCommitish: String = DEFAULT_TARGET_COMMITTISH

  private fun unsetFieldException(fieldName: String, fieldDescription: String, env: String? = null, property: String? = null): GithubReleaseException {
    val field = GithubConfig::class.memberProperties.first { it.name == fieldName }
    requireNotNull(field)
    val options = listOfNotNull(
      if (field is KMutableProperty1) "a value for `project.josm.github.$fieldName` to your Gradle build script" else null,
      if (env == null) null else "the environment variable `$env`",
      if (property == null) null else "the property `$property` in the file `${System.getProperty("user.home")}/.gradle/gradle.properties`"
    )
    require(options.isNotEmpty())

    return GithubReleaseException(
      "No $fieldDescription configured!\n  Configure it by adding/setting:\n    * " +
        options.joinToString("\n    * ")
    )
  }
}
