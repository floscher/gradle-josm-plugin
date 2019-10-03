package org.openstreetmap.josm.gradle.plugin.task.gitlab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevTag
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.gitlab.GitlabRepositorySettings
import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max

/**
 * Creates a Gitlab release from an already existing package in the Gitlab maven repository
 *
 * @param version a function that returns the version number that should be released (e.g. `1.2.3`)
 * @param names a list of package names (e.g. `org/openstreetmap/josm/gradle-josm-plugin`)
 * @property gitlabSettingsBuilder the settings defining a specific project on a specific GitLab instance
 *   where the release will be created
 */
@UnstableDefault
open class ReleaseToGitlab @Inject constructor(
  var trimLeadingV: () -> Boolean,
  var names: Set<String>
) : DefaultTask(), Runnable {

  val gitlabSettingsBuilder = GitlabRepositorySettings.Builder()

  init {
    group = "publishing"
    description = "Creates a release in GitLab from an already existing publication in a GitLab Maven package repository.\n  If this tasks runs in a GitLab CI job of a project, it is automatically configured to release for that project. Otherwise use the environment variables `GITLAB_PROJECT_ID` and `GITLAB_PERSONAL_ACCESS_TOKEN` to configure it."
  }

  @TaskAction
  override fun run() {
    require(names.isNotEmpty()) {
      "No package names given that should be included in the release!"
    }

    val gitTagName = System.getenv("GITLAB_RELEASE_GIT_TAG_NAME") ?: System.getenv("CI_COMMIT_TAG")
    val artifactVersion = System.getenv("GITLAB_RELEASE_ARTIFACT_VERSION") ?: gitTagName?.let { if (it.isNotEmpty() && trimLeadingV.invoke() && it[0].toLowerCase() == 'v') it.substring(1) else it }
    val gitlabSettings = gitlabSettingsBuilder.build()

    require(gitlabSettings != null && gitTagName != null && artifactVersion != null) {
      """
      |Set these environment variables in order to release to GitLab:
      |  GITLAB_PERSONAL_ACCESS_TOKEN    – required, a personal access token used to authenticate against the GitLab instance
      |  GITLAB_PROJECT_ID               – required except in GitLab CI, the project ID on the GitLab instance
      |  GITLAB_RELEASE_GIT_TAG_NAME     – required except in GitLab CI, the git-tag that should provide the description for the release
      |  GITLAB_RELEASE_ARTIFACT_VERSION – optional, the version of the Maven artifact published to GitLab that should be used as release artifact (defaults to GITLAB_GIT_TAG_NAME${if (trimLeadingV.invoke()) " with trimmed leading 'v'" else ""})
      |  GITLAB_URL                      – optional, the base URL of the GitLab instance (defaults to `https://gitlab.com`)
      |  GITLAB_API_URL                  – optional, the base URL of the API of the GitLab instance (defaults to `GITLAB_URL/api/v4/`)
      """.trimMargin()
    }

    // Determine project path
    val projectPath =
      System.getenv("CI_PROJECT_PATH")?.apply {
        logger.lifecycle("Using project path from environment variables: CI_PROJECT_PATH=$this")
      }
      ?: try {
        logger.lifecycle("Getting the project path from the GitLab API at ${gitlabSettings.gitlabApiUrl} …")
        Json.nonstrict.parse(ProjectInfo.serializer(), URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}").readText())
          .apply { logger.lifecycle("Retrieved project path for project ${gitlabSettings.projectId} from GitLab API: ${this.projectPath}") }.projectPath
      } catch (e: FileNotFoundException) {
        throw TaskExecutionException(this, IllegalArgumentException("Project ${gitlabSettings.projectId} not found via API ${gitlabSettings.gitlabApiUrl}!"))
      }

    logger.lifecycle("""
      Creating release for version $artifactVersion (using git tag $gitTagName):
        Project ${gitlabSettings.gitlabUrl}/$projectPath (ID ${gitlabSettings.projectId})
        Base URL for API calls: ${gitlabSettings.gitlabApiUrl}
        An API token of type ${gitlabSettings.tokenLabel} is used.
        Publishing these package names: ${names.joinToString(", ")}
    """.trimIndent())

    // Find git release
    val repo = FileRepositoryBuilder.create(File(project.rootProject.projectDir, "/.git"))
    val revTag: RevTag = Git(repo).tagList().call()
      .filter { it.name == "refs/tags/$gitTagName" }
      .map { RevWalk(repo).parseTag(it.objectId) }
      .firstOrNull() ?: throw TaskExecutionException(this, GradleException("No git tag '$gitTagName' found!"))

    logger.lifecycle("Found git tag ${revTag.tagName}: ${revTag.shortMessage}")

    // Find all matching packages available on GitLab
    val assetLinks = Json.nonstrict.parse(Package.serializer().list, URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}/packages?per_page=10000").readText())
      .filter { artifactVersion == it.version && names.contains(it.name) }
      .flatMap { packg ->
        Json.nonstrict.parse(PackageFile.serializer().list, URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}/packages/${packg.id}/package_files?per_page=10000").readText())
          .filter { it.fileName.endsWith(".jar") }
          .map { ReleaseAssetLink(it.fileName, "${gitlabSettings.gitlabUrl}/$projectPath/-/package_files/${it.id}/download") }
      }
    logger.lifecycle("Found ${assetLinks.size} *.jar release assets on GitLab: ${assetLinks.map { it.name }.joinToString(", ")}")

    // Create GitLab release
    (URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}/releases").openConnection() as HttpsURLConnection).also { connection ->
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.doInput = true
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty(gitlabSettings.tokenLabel, gitlabSettings.token)

      connection.outputStream.use {
        it.write(Json.stringify(Release.serializer(), Release(
          revTag.shortMessage,
          gitTagName,
          revTag.fullMessage
            .substring(max(0, revTag.fullMessage.indexOf('\n') + 1))
            .replace("-----BEGIN PGP SIGNATURE-----", "\n```\n-----BEGIN PGP SIGNATURE-----")
            .replace("-----END PGP SIGNATURE-----", "-----END PGP SIGNATURE-----\n```")
            .trim(),
          ReleaseAssets(assetLinks)
        )).toByteArray())
      }
      require(connection.responseCode == 201) {
        "GitLab responded with: ${connection.responseCode} ${connection.responseMessage}"
      }
    }
  }

  @Serializable
  private data class ProjectInfo(
    @SerialName("path_with_namespace") val projectPath: String
  )

  @Serializable
  private data class Package(
    val id: Int,
    val name: String,
    val version: String?
  )

  @Serializable
  private data class PackageFile(
    val id: Int,
    @SerialName("package_id") val packageId: Int,
    @SerialName("file_name") val fileName: String
  )

  @Serializable
  private data class ReleaseAssetLink(
    val name: String,
    val url: String
  )

  @Serializable
  private data class ReleaseAssets(val links: List<ReleaseAssetLink>)

  @Serializable
  private data class Release(
    val name: String,
    @SerialName("tag_name") val tagName: String,
    val description: String,
    val assets: ReleaseAssets
  )
}
