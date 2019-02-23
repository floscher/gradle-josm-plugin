package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleaseSpec
import org.openstreetmap.josm.gradle.plugin.github.get
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

const val MEDIA_TYPE_JAR = "application/java-archive"

private const val CMDLINE_OPT_LOCAL_JAR_PATH = "local-jar-path"
private const val CMDLINE_OPT_REMOTE_JAR_NAME = "remote-jar-name"

/**
 * Task to publish a release to GitHub releases.
 *
 * Note: This is currently in beta stage, so expect sudden changes to this class anytime.
 */
open class PublishToGithubReleaseTask : BaseGithubReleaseTask() {

  @Option(
    option = CMDLINE_OPT_LOCAL_JAR_PATH,
    description = "the local path to the jar which should be uploaded.\n"
      + "Default: the path of the jar built in the project")
  var localJarPath: String? = null

  @Option(
    option = CMDLINE_OPT_REMOTE_JAR_NAME,
    description = "the name of the jar after uploading.\n"
      + "Default: the name of the local jar")
  var remoteJarName: String? = null

  private val jarArchivePath: String?  by lazy {
    project.tasks.withType(Jar::class.java).getByName("jar")
      .archiveFile.get().asFile.absolutePath
  }

  private val jarArchiveName: String?  by lazy {
    project.tasks.withType(Jar::class.java).getByName("jar")
      .archiveFile.get().asFile.name
  }

  private val configuredLocalJarPath: String? by lazy {
    val notConfigured = GithubReleaseException(
      """Missing configuration for local jar to publish as release asset.
        |Configure it n the task, i.e.
        |   publishToGithubRelease {
        |       localJarPath = "/path/to/the/jar/to/publish.jar"
        |   }
        |or with the command line option --$CMDLINE_OPT_LOCAL_JAR_PATH
        """.trimMargin("|")
    )

    (if (localJarPath.isNullOrBlank()) jarArchivePath
    else localJarPath) ?: throw notConfigured
  }

  private val configuredRemoteJarName: String by lazy {
    val notConfigured = GithubReleaseException(
      """Missing configuration for remote jar name.
        |Configure it in the task, i.e.
        |   publishToGithubRelease {
        |       remoteJarName = "my-josm-plugin.jar"
        |   }
        |or with the command line option --$CMDLINE_OPT_REMOTE_JAR_NAME
        """.trimMargin("|")
    )
    (if (remoteJarName.isNullOrBlank()) null else remoteJarName)
      ?: jarArchiveName
      ?: throw notConfigured
  }

  /**
   * throws an exception if this file isn't a valid jar file with a manifest
   * including
   *   * an attribute `Plugin-Version`
   *   * an attribute `Plugin-Mainversion`
   *
   * Both must match the respective attributes in [release].
   *
   * @exception GithubReleaseException thrown, if this file isn't
   * consistent with the release spec
   */
  @Throws(GithubReleaseException::class)
  private fun File.ensureConsistentWithReleaseSpec(release: ReleaseSpec) {
    val jarFile = try {
      JarFile(this)
    } catch (e: IOException) {
      null
    } ?: throw GithubReleaseException(
      "The file ${this.absolutePath} isn't a valid jar file."
    )
    val manifest = jarFile.manifest ?: throw GithubReleaseException(
      "The jar file ${this.absolutePath} doesn't include a MANIFEST file"
    )
    val pluginVersion = manifest.mainAttributes.getValue("Plugin-Version")?.trim()
    if (pluginVersion.isNullOrEmpty()) {
      throw GithubReleaseException(
        """The jar file '${this.absolutePath}' doesn't include an
          |attribute 'Plugin-Version'""""
          .trimMargin()
      )
    } else if (pluginVersion != release.label) {
      throw GithubReleaseException(
        """The plugin version in the in the MANIFEST of the jar file
          |'${this.absolutePath}' doesn't match with the release label
          | of the release.
          | Release label:                  ${release.label}
          | Plugin-version in the MANIFEST: $pluginVersion
          """.trimMargin()
      )
    }

    val josmVersion = try {
      manifest.mainAttributes.getValue("Plugin-Mainversion")?.trim()?.toInt()
    } catch (e: Throwable) {
      null
    } ?: throw GithubReleaseException(
      """The jar file '${this.absolutePath}' does either not include
        |an attribute 'Plugin-Main-Version' or its value isn't a
        |positive number""""
        .trimMargin()
    )
    if (josmVersion != release.minJosmVersion) {
      throw GithubReleaseException(
        """The minimum JOSM version in the MANIFEST of the jar file
          |'${this.absolutePath}' doesn't match with the minimum JOSM version
          | of the release.
          | Version for the release: ${release.minJosmVersion}
          | Version in the MANIFEST: $josmVersion"""
          .trimMargin()
      )
    }
  }

  private fun deleteExistingReleaseAssetForName(releaseId: Int, name: String) {
    val apiClient = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.apiUrl)
    val assets = apiClient.getReleaseAssets(releaseId)
    // not sure whether we can have multiple assets with the same 'name'.
    // Just in case: use 'filter' and 'forEach' instead of
    // 'first' and 'let'
    assets.filter {  it["name"] == name}.forEach {asset ->
      val assetId = asset["id"].toString().toInt()
      apiClient.deleteReleaseAsset(assetId)
      logger.lifecycle("Deleted already existing release asset '$name' with id '$assetId'")
    }
  }

  @TaskAction
  fun publishToGithubRelease() {

    val releaseLabel = configuredReleaseLabel
    val githubClient = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.apiUrl)

    val releaseConfig = ReleaseSpec.loadListFrom(project.extensions.josm.github.releasesConfig.inputStream())

    val notFound = GithubReleaseException(
      """The releases config file '${project.extensions.josm.github.releasesConfig}'
        |doesn't include a release with release label '$releaseLabel' yet.
        |Add and configure a release with this label in
        |  '${project.extensions.josm.github.releasesConfig}'
        |and rerun."""
        .trimMargin("|")
    )

    val localRelease = releaseConfig[releaseLabel] ?: throw notFound

    val remoteReleases = githubClient.getReleases()

    val remoteReleaseNotFound = GithubReleaseException("""
      |Remote release with label '$releaseLabel' doesn't
      |exist on the GitHub server.
      |Can't upload release jar to the release '$releaseLabel.
      |Create release '$releaseLabel' first, i.e.
      |  ./gradlew createGithubRelease --release-label $releaseLabel
      """.trimMargin())
    val remoteRelease = remoteReleases
      .find {it["tag_name"] == releaseLabel}
      ?: throw remoteReleaseNotFound

    val releaseId = remoteRelease["id"].toString().toInt()
    val localFile = File(configuredLocalJarPath)
    if (!localFile.exists() || !localFile.isFile || !localFile.canRead()) {
      throw GithubReleaseException(
        "Local jar file '$configuredLocalJarPath' doesn't exist " +
        "or isn't readable. Can't upload it as release asset."
      )
    }
    localFile.ensureConsistentWithReleaseSpec(localRelease)

    deleteExistingReleaseAssetForName(releaseId, configuredRemoteJarName)

    val githubUploadClient = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.uploadUrl)
    val asset = githubUploadClient.uploadReleaseAsset(
      releaseId = releaseId,
      name = configuredRemoteJarName,
      contentType = MEDIA_TYPE_JAR,
      file = localFile
    )
    logger.lifecycle(
      "Uploaded '{}' to release '{}' with asset name '{}'. Asset id is '{}'.",
      localFile.name,
      releaseLabel,
      configuredRemoteJarName,
      asset["id"]
    )
  }
}
