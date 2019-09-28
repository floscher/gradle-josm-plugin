package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.Project

private const val DEFAULT_JOB_TOKEN_ENV = "CI_JOB_TOKEN"
private const val DEFAULT_PAT_ENV = "PERSONAL_ACCESS_TOKEN"

class GitlabConfig() {

  /**
   * A map of GitLab domains to the project ID of the repository on each GitLab instance
   *
   * @since 0.6.2
   */
  val repositories: MutableList<GitlabRepository> = mutableListOf()

  data class GitlabRepository(
    val domain: String,
    val projectId: Int,
    val jobTokenEnv: String? = DEFAULT_JOB_TOKEN_ENV,
    val personalAccessTokenEnv: String? = DEFAULT_PAT_ENV
  )

  @JvmOverloads
  fun repository(
    domain: String,
    projectId: Int,
    jobTokenEnv: String? = DEFAULT_JOB_TOKEN_ENV,
    personalAccessTokenEnv: String? = DEFAULT_PAT_ENV
  ) = repositories.add(GitlabRepository(domain, projectId, jobTokenEnv, personalAccessTokenEnv))
}
