package org.openstreetmap.josm.gradle.plugin.gitlab

import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.openstreetmap.josm.gradle.plugin.config.GitlabConfig
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension

/**
 * Adds the given [GitlabConfig.GitlabRepository] to the [JosmPluginExtension.publishRepositories].
 */
fun JosmPluginExtension.addGitlabMavenRepository(
  gitlabRepository: GitlabConfig.GitlabRepository,
  repoName: String
) {
  val credentialHeader =
    System.getenv(gitlabRepository.personalAccessTokenEnv)?.let { "Private-Token" to it }
      ?: System.getenv(gitlabRepository.jobTokenEnv)?.let { "Job-Token" to it }

  if (credentialHeader == null) {
    project.logger.lifecycle("No credentials are provided for the GitLab repository for project ${gitlabRepository.projectId}@${gitlabRepository.domain} !"
      + " Set environment variable ${gitlabRepository.jobTokenEnv} (job token) or ${gitlabRepository.personalAccessTokenEnv} (personal access token).")
  } else {
    val prevValue = publishRepositories
    publishRepositories = {
      prevValue.invoke(it)
      it.maven {
        it.url = project.uri("https://${gitlabRepository.domain}/api/v4/projects/${gitlabRepository.projectId}/packages/maven")
        it.name = repoName

        it.credentials(HttpHeaderCredentials::class.java) {
          it.name = credentialHeader.first
          it.value = credentialHeader.second
        }

        it.authentication {
          it.removeAll { true }
          it.create("auth", HttpHeaderAuthentication::class.java)
        }
      }
    }
  }
}
