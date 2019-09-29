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
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max

/**
 * Creates a Gitlab release from an already existing package in the Gitlab maven repository
 *
 * @param version a function that returns the version number that should be released (e.g. `1.2.3`)
 * @param names a list of package names (e.g. `org/openstreetmap/josm/gradle-josm-plugin`)
 * @property gitlabSettings the settings defining a specific project on a specific GitLab instance
 *   where the release will be created
 */
@UnstableDefault
open class ReleaseToGitlab @Inject constructor(
  var version: () -> Any,
  var names: Set<String>
) : DefaultTask(), Runnable {

  val gitlabSettings = GitlabRepositorySettings.Builder()

  init {
    group = "publishing"
    description = "Creates a release in GitLab from an already existing publication in a GitLab Maven package repository.\n  If this tasks runs in a GitLab CI job of a project, it is automatically configured to release for that project. Otherwise use the environment variables `GITLAB_PROJECT_ID` and `GITLAB_PERSONAL_ACCESS_TOKEN` to configure it."
  }

  @TaskAction
  override fun run() {
    require(names.isNotEmpty()) {
      "No package names given that should be included in the release!"
    }

    val gitlabSettings = gitlabSettings.build()
    val version = this.version.invoke().toString()

    // Determine project path
    val projectPath = System.getenv("CI_PROJECT_PATH")?.apply {
      logger.lifecycle("Using project path from environment variables: CI_PROJECT_PATH=$this")
    } ?: Json.nonstrict.parse(ProjectInfo.serializer(), URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}").readText())
        .apply { logger.lifecycle("Retrieved project path for project ${gitlabSettings.projectId} from GitLab API: $this") }

    logger.lifecycle("""
      Creating release for version $version:
        Project ${gitlabSettings.gitlabUrl}/$projectPath (ID ${gitlabSettings.projectId})
        Base URL for API calls: ${gitlabSettings.gitlabApiUrl}
        An API token of type ${gitlabSettings.tokenLabel} is used.
    """.trimIndent())

    // Find git release
    val repo = FileRepositoryBuilder.create(File(project.rootProject.projectDir, "/.git"))
    val revTag: RevTag = Git(repo).tagList().call()
      .filter { it.name == "refs/tags/$version" }
      .map { RevWalk(repo).parseTag(it.objectId) }
      .firstOrNull() ?: throw TaskExecutionException(this, GradleException("No git tag found for version v$version!"))

    logger.lifecycle("""
      Found git tag ${revTag.tagName}:
        ${revTag.shortMessage}
          ${revTag.fullMessage.lines().joinToString("\n    ")}
    """.trimIndent())

    // Find all matching packages available on GitLab
    val assetLinks = Json.nonstrict.parse(Package.serializer().list, URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}/packages").readText())
      .filter { version == it.version && names.contains(it.name) }
      .flatMap { packg ->
        Json.nonstrict.parse(PackageFile.serializer().list, URL("${gitlabSettings.gitlabApiUrl}/projects/${gitlabSettings.projectId}/packages/${packg.id}/package_files").readText())
          .filter { it.fileName.endsWith(".jar") }
          .map { ReleaseAssetLink(it.fileName, "${gitlabSettings.gitlabUrl}/$projectPath/-/package_files/${it.id}/download") }
      }
    logger.lifecycle("""
      Found ${assetLinks.size} *.jar release assets on GitLab:
        * ${assetLinks.map { it.name }.joinToString("\n  * ")}
      """.trimIndent())

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
          version,
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
