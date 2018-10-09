package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.github.DEFAULT_PICKUP_RELEASE_LABEL
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleaseSpec
import org.openstreetmap.josm.gradle.plugin.github.ReleasesSpec
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

const val MEDIA_TYPE_JAR = "application/java-archive"

private const val CMDLINE_OPT_LOCAL_JAR_PATH = "local-jar-path"
private const val CMDLINE_OPT_PUBLISH_TO_PICKUP_RELEASE = "publish-to-pickup-release"
private const val CMDLINE_OPT_REMOTE_JAR_NAME = "remote-jar-name"

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

  @Option(
    option = CMDLINE_OPT_PUBLISH_TO_PICKUP_RELEASE,
    description =
    "indicates whether the asset is also updated to the pickup release.\n"
      + "The label of the pickup release is configured in 'releases.yml' and "
      + "defaults to '$DEFAULT_PICKUP_RELEASE_LABEL'.\n"
  )
  var publishToPickupRelease: Boolean? = null

  private val jarArchivePath: String?  by lazy {
    project.tasks.withType(Jar::class.java).getByName("jar")
      .archivePath.absolutePath
  }

  private val jarArchiveName: String?  by lazy {
    project.tasks.withType(Jar::class.java).getByName("jar")
      .archivePath.name
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

  private val configuredPublishToPickupRelease: Boolean by lazy {
    publishToPickupRelease ?: false
  }

  /**
   * true, if this file can be uploaded to a github release, i.e. if it
   * is an existing, readable, locally available file
   */
  private val File.canUpload: Boolean
    get() = this.exists() && this.isFile && this.canRead()

  /**
   * true, if this is a valid JAR file
   */
  internal val File.isJar: Boolean
    get() = try {
      JarFile(this)
      true
    } catch(e:Throwable) {
      false
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
          .trimMargin("|")
      )
    } else if (pluginVersion != release.label) {
      throw GithubReleaseException(
        """The plugin version in the in the MANIFEST of the jar file
          |'${this.absolutePath}' doesn't match with the release label
          | of the release.
          | Release label:                  ${release.label}
          | Plugin-version in the MANIFEST: $pluginVersion
          """.trimMargin("|")
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
        .trimMargin("|")
    )
    if (josmVersion != release.numericJosmVersion) {
      throw GithubReleaseException(
        """The numeric JOSM version in the MANIFEST of the jar file
          |'${this.absolutePath}' doesn't match with the numeric JOSM version
          | of the release.
          | Numeric JOSM version for the release: ${release.numericJosmVersion}
          | Numeric JOSM version in the MANIFEST: $josmVersion"""
          .trimMargin("|")
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

    val releaseConfig = ReleasesSpec.load(project.extensions.josm.github.releasesConfig)

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

    val remoteReleaseNotFound = GithubReleaseException
      .remoteReleaseDoesntExist(releaseLabel)
    val remoteRelease = remoteReleases
      .find {it["tag_name"] == releaseLabel}
      ?: throw remoteReleaseNotFound

    val releaseId = remoteRelease["id"].toString().toInt()
    val localFile = File(configuredLocalJarPath)
    if (!localFile.canUpload) {
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

    if (configuredPublishToPickupRelease) {

      val pickupReleaseLabel = releaseConfig.pickupRelease.label
      val pickupReleaseNotFound = GithubReleaseException
        .remotePickupReleaseDoesntExit(pickupReleaseLabel)

      val remotePickupRelease = remoteReleases
        .find {it["tag_name"] == pickupReleaseLabel}
        ?: throw pickupReleaseNotFound

      val pickupReleaseId = remotePickupRelease["id"].toString().toInt()
      deleteExistingReleaseAssetForName(pickupReleaseId,
        configuredRemoteJarName)

      val pickupReleaseBody = releaseConfig.pickupRelease
        .descriptionForPickedUpRelease(
          pickedUpRelase =  localRelease,
          pickedUpReleaseUrl = project.extensions.josm.github.getReleaseUrl(localRelease.label)
        )

      githubClient.updateRelease(
        releaseId = pickupReleaseId,
        body = pickupReleaseBody)

      val latestReleaseAsset = githubUploadClient.uploadReleaseAsset(
        releaseId = pickupReleaseId,
        name = configuredRemoteJarName,
        contentType = MEDIA_TYPE_JAR,
        file = localFile
      )

      logger.lifecycle(
        "Uploaded '{}' to release '{}' with asset name '{}'. Asset id is '{}'.",
        localFile.name,
        pickupReleaseLabel,
        configuredRemoteJarName,
        latestReleaseAsset["id"]
      )
    }
  }
}
