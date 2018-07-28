package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_API_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_UPLOAD_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.GithubReleasesClient
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

class GithubReleaseTaskException(override var message: String,
                                 override var cause: Throwable?)
    : Exception(message, cause) {

    constructor(cause: Throwable) : this(cause.message ?: "", cause)
    constructor(message: String) : this(message, null)

    companion object {
        fun remoteReleaseDoesntExist(releaseLabel: String)
                : GithubReleaseTaskException {
            val msg = """Remote release with label '$releaseLabel' doesn't
                |exist on the GitHub server.
                |Can't upload release jar to the release '$releaseLabel.
                |Create release '$releaseLabel' first, i.e.
                |  ./gradlew createGithubRelease --release-label $releaseLabel
                """.trimMargin("|")
            return GithubReleaseTaskException(msg)
        }

        fun remotePickupReleaseDoesntExit(releaseLabel: String)
            : GithubReleaseTaskException {
            val msg = """Remote pickup release with label '$releaseLabel'
                |doesn't exist on the GitHub server.
                |Can't upload release jar to the pickup release '$releaseLabel'.
                |Create pickup release first, i.e.
                |   ./gradlew createPickupRelease
                |"""
                .trimMargin("|")
            return GithubReleaseTaskException(msg)
        }
    }
}

const val DEFAULT_TARGET_COMMITISH = "master"
const val MEDIA_TYPE_JAR = "application/java-archive"

// config options
const val CONFIG_OPT_GITHUB_USER = "josm.github.user"
const val CONFIG_OPT_GITHUB_ACCESS_TOKEN = "josm.github.access_token"
const val CONFIG_OPT_GITHUB_API_URL = "josm.github.api_url"
const val CONFIG_OPT_GITHUB_UPLOAD_URL = "josm.github.upload_url"
const val CONFIG_OPT_GITHUB_URL = "josm.github.url"
const val CONFIG_OPT_GITHUB_REPOSITORY = "josm.github.repository"
const val CONFIG_OPT_RELEASES_CONFIG_FILE = "josm.releases_config_file"
const val CONFIG_OPT_TARGET_COMMITISH = "josm.target_commitish"

// environment variables
const val ENV_VAR_GITHUB_USER = "GITHUB_USER"
const val ENV_VAR_GITHUB_ACCESS_TOKEN = "GITHUB_ACCESS_TOKEN"
const val ENV_VAR_GITHUB_REPOSITORY = "GITHUB_REPOSITORY"
const val ENV_VAR_GITHUB_API_URL = "GITHUB_API_URL"
const val ENV_VAR_GITHUB_UPLOAD_URL = "GITHUB_UPLOAD_URL"
const val ENV_VAR_GITHUB_URL = "GITHUB_URL"

// command line options
const val CMDLINE_OPT_RELEASE_LABEL = "release-label"
const val CMDLINE_OPT_TARGET_COMMITISH = "target-commitish"
const val CMDLINE_OPT_LOCAL_JAR_PATH = "local-jar-path"
const val CMDLINE_OPT_REMOTE_JAR_NAME = "remote-jar-name"
const val CMDLINE_OPT_PUBLISH_TO_PICKUP_RELEASE = "publish-to-pickup-release"

private fun Project.lookupConfiguredProperty(propertyName: String,
                                     envName: String? = null): String? {
    val prop = this.findProperty(propertyName)?.toString()?.trim()

    return if (!prop.isNullOrEmpty()) { prop }
    else {
        envName?.let {
            System.getenv(envName)?.trim().let {value->
                if (value.isNullOrEmpty()) null else value
            }
        }
    }
}

val Project.configuredGithubUser: String get() {
    val notConfigured = GithubReleaseTaskException(
        """No github user name configured.
          |Configure it
          |- with the project property $CONFIG_OPT_GITHUB_USER
          |- or with the environment variable $ENV_VAR_GITHUB_USER"""
            .trimMargin("|")
    )

    return this.lookupConfiguredProperty(
        propertyName = CONFIG_OPT_GITHUB_USER,
        envName = ENV_VAR_GITHUB_USER
    ) ?: throw notConfigured
}

val Project.configuredGithubAccessToken: String get() {
    val notConfigured = GithubReleaseTaskException(
        """No github access token configured.
            |Configure it
            |- with the project property $CONFIG_OPT_GITHUB_ACCESS_TOKEN
            |- or with the environment variable $ENV_VAR_GITHUB_ACCESS_TOKEN"""
            .trimMargin("|")
    )

    return lookupConfiguredProperty(
        propertyName =  CONFIG_OPT_GITHUB_ACCESS_TOKEN,
        envName = ENV_VAR_GITHUB_ACCESS_TOKEN
    ) ?: throw notConfigured
}

val Project.defaultReleasesConfigFile: File get() {
    return File(this.project.projectDir, "releases.yml")
}

fun Project.releaseUrl(label: String) =
    "${this.configuredGithubUrl}/${this.configuredGithubUser}/" +
        "${this.configuredGithubRepository}/releases/tag/$label"

/**
 * the configured release config file
 */
val Project.configuredReleasesConfigFile: File get() {
    fun ensureReleaseConfigFileReadable(configFile: File) {
        if (! (configFile.isFile && configFile.exists()
                && configFile.canRead())) {
            throw IOException(
                "releases configuration file <${configFile.absolutePath}> "
                    + "doesn't exist or can't be read")
        }
    }

    val file =
        lookupConfiguredProperty(
            propertyName = CONFIG_OPT_RELEASES_CONFIG_FILE
        )?.trim().let {name ->
            if (name.isNullOrEmpty()) null else File(name)
        } ?: defaultReleasesConfigFile

    ensureReleaseConfigFileReadable(file)
    return file
}

val Project.configuredGithubApiUrl: String get () {
    return lookupConfiguredProperty(
            propertyName = CONFIG_OPT_GITHUB_API_URL,
            envName = ENV_VAR_GITHUB_API_URL
    ) ?: DEFAULT_GITHUB_API_URL
}

val Project.configuredGithubUploadUrl: String get () {
    return lookupConfiguredProperty(
        propertyName = CONFIG_OPT_GITHUB_UPLOAD_URL,
        envName = ENV_VAR_GITHUB_UPLOAD_URL
    ) ?: DEFAULT_GITHUB_UPLOAD_URL
}

val Project.configuredGithubUrl: String get () {
    return lookupConfiguredProperty(
        propertyName = CONFIG_OPT_GITHUB_URL,
        envName = ENV_VAR_GITHUB_URL
    ) ?: DEFAULT_GITHUB_URL
}

val Project.configuredGithubRepository: String get () {
    return lookupConfiguredProperty(
        propertyName = CONFIG_OPT_GITHUB_REPOSITORY,
        envName = ENV_VAR_GITHUB_REPOSITORY
    ) ?: project.name
}


/**
 * Base class for tasks related to the management of github releases
 */
open class BaseGithubReleaseTask: DefaultTask() {
    @Option(
        option = CMDLINE_OPT_RELEASE_LABEL,
        description = "the release label. Example: v0.0.1")
    var releaseLabel: String? = null

    @Option(
        option = CMDLINE_OPT_TARGET_COMMITISH,
        description = "the target commitish for the release, i.e. 'master' "
            + "or 'deploy'. Default: 'master'")
    var targetCommitish: String? = null

    val configuredReleaseLabel: String by lazy {
        val notConfigured = GithubReleaseTaskException(
            """Release label not configured or blank.
            |Configure it in the task, i.e.
            |   createGithubRelease {
            |       releaseLabel = "v1.0.0"
            |   }
            |or set the project property 'version', i.e.
            |   version = "v1.0.0"
            |or use the command line option --$CMDLINE_OPT_RELEASE_LABEL"""
                  .trimMargin("|")
        )

        fun labelFromVersion() : String? {
            val version = project.findProperty("version")?.toString()
            return if (version.isNullOrBlank()) null else version
        }

        (if (releaseLabel.isNullOrBlank()) null else releaseLabel)
        ?: labelFromVersion()
        ?: throw notConfigured
    }

    val configuredTargetCommitish: String by lazy {
        (if (targetCommitish.isNullOrBlank()) null
         else targetCommitish
        )
        ?:project.lookupConfiguredProperty(
             propertyName = CONFIG_OPT_TARGET_COMMITISH
          )
        ?: DEFAULT_TARGET_COMMITISH
    }

    protected fun githubReleaseClient(
            url: String = project.configuredGithubApiUrl) =
        GithubReleasesClient(
            repository = project.configuredGithubRepository,
            user = this.project.configuredGithubUser,
            accessToken = this.project.configuredGithubAccessToken,
            apiUrl = url
        )
}

/**
 * Task to create a github release using the github API.
 */
open class CreateGithubReleaseTask : BaseGithubReleaseTask() {

    @TaskAction
    fun createGithubRelease() {
        val releaseConfigFile = project.configuredReleasesConfigFile
        val releaseLabel = configuredReleaseLabel
        val releaseConfig = ReleasesSpec.load(releaseConfigFile)

        val notFound = GithubReleaseTaskException(
        """The releases config file '$releaseConfigFile' doesn't include a
            |release with release label '$releaseLabel yet. Add and configure
            |a release with this label in '$releaseConfigFile' and rerun."""
            .trimMargin("|")
        )
        val release = releaseConfig[releaseLabel] ?: throw notFound

        val client = githubReleaseClient()

        client.getReleases().find {it["tag_name"] == releaseLabel}?.let {
            throw GithubReleaseTaskException(
                "Release with release label '$releaseLabel' already exists "
                    + "on the GitHub server."
            )
        }

        try {
            client.createRelease(
                tagName = releaseLabel,
                targetCommitish = configuredTargetCommitish,
                name = release.name,
                body = release.description)
            logger.lifecycle("New release '{}' created in GitHub repository",
                releaseLabel)
        } catch(e: Throwable) {
           throw GithubReleaseTaskException(e)
        }
    }
}

open class CreatePickupReleaseTask: BaseGithubReleaseTask() {

    @TaskAction
    fun createPickupRelease() {
        val releaseConfigFile = project.configuredReleasesConfigFile
        val releaseConfig = ReleasesSpec.load(releaseConfigFile)

        val release = releaseConfig.pickupRelease
        val client = githubReleaseClient()

        client.getReleases().find {it["tag_name"] == release.label}?.let {
            throw GithubReleaseTaskException(
                "Pickup release with label '${release.label}' already exists "
                    + "on the GitHub server."
            )
        }

        try {
            client.createRelease(
                tagName = release.label,
                targetCommitish = configuredTargetCommitish,
                name = release.name,
                body = release.defaultDescriptionForPickupRelease())
            logger.lifecycle("Pickup release '{}' created in GitHub repository",
                release.label)
        } catch(e: Throwable) {
            throw GithubReleaseTaskException(e)
        }
    }
}

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
        val notConfigured = GithubReleaseTaskException(
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
        val notConfigured = GithubReleaseTaskException(
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
     * @exception GithubReleaseTaskException thrown, if this file isn't
     * consistent with the release spec
     */
    @Throws(GithubReleaseTaskException::class)
    private fun File.ensureConsistentWithReleaseSpec(release: ReleaseSpec) {
        val jarFile = try {
            JarFile(this)
        } catch (e: IOException) {
            null
        } ?: throw GithubReleaseTaskException(
            "The file ${this.absolutePath} isn't a valid jar file."
        )
        val manifest = jarFile.manifest ?: throw GithubReleaseTaskException(
            "The jar file ${this.absolutePath} doesn't include a MANIFEST file"
            )
        val pluginVersion = manifest.mainAttributes.getValue("Plugin-Version")
            ?.trim()
        if (pluginVersion.isNullOrEmpty()) {
            throw GithubReleaseTaskException(
                """The jar file '${this.absolutePath}' doesn't include an
                |attribute 'Plugin-Version'""""
                    .trimMargin("|")
            )
        } else if (pluginVersion != release.label) {
            throw GithubReleaseTaskException(
            """The plugin version in the in the MANIFEST of the jar file
            |'${this.absolutePath}' doesn't match with the release label
            | of the release.
            | Release label:                  ${release.label}
            | Plugin-version in the MANIFEST: $pluginVersion
            """.trimMargin("|")
            )
        }

        val josmVersion = try {
            manifest.mainAttributes.getValue("Plugin-Mainversion")?.trim()
                ?.toInt()
        } catch (e: Throwable) {
            null
        } ?: throw GithubReleaseTaskException(
            """The jar file '${this.absolutePath}' does either not include
            |an attribute 'Plugin-Main-Version' or its value isn't a
            |positive number""""
                .trimMargin("|")
        )
        if (josmVersion != release.numericJosmVersion) {
            throw GithubReleaseTaskException(
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
        val apiClient = githubReleaseClient()
        val assets = apiClient.getReleaseAssets(releaseId)
        // not sure whether we can have multiple assets with the same 'name'.
        // Just in case: use 'filter' and 'forEach' instead of
        // 'first' and 'let'
        assets.filter {  it["name"] == name}.forEach {asset ->
            val assetId = asset["id"].toString().toInt()
            apiClient.deleteReleaseAsset(assetId)
            logger.lifecycle("Deleted already existing release asset " +
                "'$name' with id '$assetId'")
        }
    }

    @TaskAction
    fun publishToGithubRelease() {

        val releaseLabel = configuredReleaseLabel
        val githubClient = githubReleaseClient()

        val releaseConfig = ReleasesSpec.load(
            project.configuredReleasesConfigFile)

        val notFound = GithubReleaseTaskException(
        """The releases config file '${project.configuredReleasesConfigFile}'
            |doesn't include a release with release label '$releaseLabel' yet.
            |Add and configure a release with this label in
            |  '${project.configuredReleasesConfigFile}'
            |and rerun."""
            .trimMargin("|")
        )

        val localRelease = releaseConfig[releaseLabel] ?: throw notFound

        val remoteReleases = githubClient.getReleases()

        val remoteReleaseNotFound = GithubReleaseTaskException
            .remoteReleaseDoesntExist(releaseLabel)
        val remoteRelease = remoteReleases
            .find {it["tag_name"] == releaseLabel}
            ?: throw remoteReleaseNotFound

        val releaseId = remoteRelease["id"].toString().toInt()
        val localFile = File(configuredLocalJarPath)
        if (!localFile.canUpload) {
            throw GithubReleaseTaskException(
              "Local jar file '$configuredLocalJarPath' doesn't exist " +
              "or isn't readable. Can't upload it as release asset."
            )
        }
        localFile.ensureConsistentWithReleaseSpec(localRelease)

        deleteExistingReleaseAssetForName(releaseId, configuredRemoteJarName)

        val githubUploadClient = githubReleaseClient(
            project.configuredGithubUploadUrl)
        val asset = githubUploadClient.uploadReleaseAsset(
            releaseId = releaseId,
            name = configuredRemoteJarName,
            contentType = MEDIA_TYPE_JAR,
            file = localFile
        )
        logger.lifecycle(
            "Uploaded '{}' to release '{}' with asset name '{}'. " +
            "Asset id is '{}'.",
            localFile.name,
            releaseLabel,
            configuredRemoteJarName,
            asset["id"]
        )

        if (configuredPublishToPickupRelease) {

            val pickupReleaseLabel = releaseConfig.pickupRelease.label
            val pickupReleaseNotFound = GithubReleaseTaskException
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
                    pickedUpReleaseUrl = project.releaseUrl(localRelease.label)
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

            logger.lifecycle("Uploaded '{}' to release '{}' with asset " +
                "name '{}'. Asset id is '{}'.",
                localFile.name,
                pickupReleaseLabel,
                configuredRemoteJarName,
                latestReleaseAsset["id"]
            )
        }
    }
}
