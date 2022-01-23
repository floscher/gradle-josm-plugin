package org.openstreetmap.josm.gradle.plugin.api.gitlab

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.openstreetmap.josm.gradle.plugin.api.gitlab.GitlabRepositorySettings.Builder
import java.net.URI

/**
 * The GitLab base URL that is used when no other URL is configured.
 * The value is `https://gitlab.com`.
 */
public const val DEFAULT_GITLAB_URL: String = "https://gitlab.com"

/**
 * A configuration consisting of one specific project on one specific GitLab instance
 * and a token that can be used to make API requests.
 * Use [Builder.build] to create an instance.
 *
 * @property projectId the ID of the project on the GitLab instance
 * @property gitlabUrl the base URL of the GitLab instance
 * @property gitlabApiUrl the base URL of the API of the GitLab instance
 * @property tokenLabel if the token is a CI job token, this is "Job-Token", otherwise this is "Private-Token"
 * @property token the token to use when authenticating
 */
public class GitlabRepositorySettings private constructor(
  public val projectId: Int,
  public val gitlabUrl: String,
  public val gitlabApiUrl: String,
  public val tokenLabel: String,
  public val token: String
) {
  /**
   * A builder to create a [GitlabRepositorySettings] object.
   * In GitLab CI, the values are automatically set via environment variables.
   * Otherwise you can set the following environment variables:
   * * `GITLAB_PROJECT_ID`
   * * `GITLAB_PERSONAL_ACCESS_TOKEN`
   * * `GITLAB_URL` (if not set, `https://gitlab.com` is used)
   *
   * @property projectId the ID of the project on the GitLab server
   *
   *   **Defaults (first not-null integer value is used):**
   *   * env-var `GITLAB_PROJECT_ID` (intended to be set manually by user)
   *   * env-var `CI_PROJECT_ID` (is present in GitLab CI jobs)
   * @property gitlabUrl the base URL (e.g. `https://gitlab.com`) of the GitLab instance
   *
   *   **Defaults (first not-null value is used):**
   *   * env-var `GITLAB_URL` (intended to be set manually by user)
   *   * "https://" + env-var `CI_SERVER_HOST` (is present in GitLab CI jobs)
   *   * [DEFAULT_GITLAB_URL]
   * @property gitlabApiUrl the base URL of the GitLab API (e.g. `https://gitlab.com/api/v4`)
   *
   *   **Defaults (first not-null value is used):**
   *   * env-var `GITLAB_API_URL` (intended to be set manually by user)
   *   * env-var `CI_API_V4_URL` (is present in GitLab CI jobs)
   *   * [gitlabUrl] + "/api/v4"
   * @property ciJobToken the token of the CI job
   *
   *   **Default:**
   *   * env-var `CI_JOB_TOKEN` (is present in GitLab CI jobs)
   * @property personalAccessToken a token of a GitLab user that will be used to authenticate against the GitLab API, overrides the [ciJobToken] if set
   *
   *   **Default:**
   *   * env-var `GITLAB_PERSONAL_ACCESS_TOKEN` (intended to be set manually by user)
   */
  public data class Builder @JvmOverloads constructor(
    val project: Project,
    var projectId: Int? = project.providers.environmentVariable("GITLAB_PROJECT_ID").forUseAtConfigurationTime().orNull?.toIntOrNull()
      ?: project.providers.environmentVariable("CI_PROJECT_ID").forUseAtConfigurationTime().orNull?.toIntOrNull(),
    var gitlabUrl: String = project.providers.environmentVariable("GITLAB_URL").orNull
      ?: project.providers.environmentVariable("CI_SERVER_HOST").orNull?.let { "https://$it" }
      ?: DEFAULT_GITLAB_URL,
    var gitlabApiUrl: String = project.providers.environmentVariable("GITLAB_API_URL").orNull
      ?: project.providers.environmentVariable("CI_API_V4_URL").orNull
      ?: "$gitlabUrl/api/v4",
    var ciJobToken: String? = project.providers.environmentVariable("CI_JOB_TOKEN").orNull,
    var personalAccessToken: String? = project.providers.environmentVariable("GITLAB_PERSONAL_ACCESS_TOKEN").orNull
  ) {
    public fun build(): GitlabRepositorySettings? {
      // Can be null
      val ciJobToken = ciJobToken
      // Check if these are null
      val projectId = projectId
      val token = personalAccessToken ?: ciJobToken

      return if (projectId != null && token != null) {
        GitlabRepositorySettings(projectId, gitlabUrl, gitlabApiUrl, if (personalAccessToken != null) "Private-Token" else "Job-Token", token)
      } else {
        null
      }
    }
  }
}

/**
 * Adds a Gitlab.com Maven package repository named [repoName] to the [RepositoryHandler].
 * If you publish in a GitLab CI job, the repository will be automatically configured.
 * Otherwise you can use environment variables to configure the repository
 * (see [GitlabRepositorySettings.Builder] for the available options).
 */
public fun RepositoryHandler.gitlabRepository(repoName: String, project: Project): MavenArtifactRepository? {
  val repo = gitlabRepositoryConfiguration(project, repoName)?.let {
    maven(it)
  }
  if (repo == null) {
    project.logger.lifecycle(GITLAB_ENV_VAR_HINT)
  }
  return repo
}

private fun gitlabRepositoryConfiguration(project: Project, repoName: String): ((MavenArtifactRepository).() -> Unit)? =
  Builder(project).build()?.let { settings ->
    {
      url = URI("${settings.gitlabApiUrl}/projects/${settings.projectId}/packages/maven")
      name = repoName
      credentials(HttpHeaderCredentials::class.java) {
        it.name = settings.tokenLabel
        it.value = settings.token
      }
      authentication {
        it.removeAll { true }
        it.create("auth", HttpHeaderAuthentication::class.java)
      }
    }
  }

public fun Project.setupGitlabPublishing(repoName: String) {
  plugins.withType(PublishingPlugin::class.java).whenPluginAdded {
    gitlabRepositoryConfiguration(this, repoName)?.let { gitlabRepoConf ->
      extensions.getByType(PublishingExtension::class.java).repositories { r ->
        // Create GitLab Maven repository to publish to.
        r.maven(gitlabRepoConf)
      }
    } ?: logger.lifecycle(GITLAB_ENV_VAR_HINT)
  }
}

private const val GITLAB_ENV_VAR_HINT: String =
  "Note: If you set the environment variables GITLAB_PROJECT_ID and GITLAB_PERSONAL_ACCESS_TOKEN, " +
  "then you can publish your Maven artifacts to a gitlab.com repository. " +
  "In GitLab CI these environment variables are set automatically."
