package org.openstreetmap.josm.gradle.plugin.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_API_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.GithubReleasesClient
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

class GithubReleaseTaskException(override var message: String, override var cause: Throwable?)
    : Exception(message, cause) {
    constructor(message: String) : this(message, null) {
    }
}

const val DEFAULT_TARGET_COMMITISH = "master"
const val MEDIA_TYPE_JAR = "application/java-archive"

// config options
const val CONFIG_OPT_GITHUB_USER = "josm.github.user"
const val CONFIG_OPT_GITHUB_ACCESS_TOKEN = "josm.github.access_token"
const val CONFIG_OPT_GITHUB_API_URL = "josm.github.api_url"
const val CONFIG_OPT_GITHUB_REPOSITORY = "josm.github.repository"
const val CONFIG_OPT_RELEASES_CONFIG_FILE = "josm.releases_config_file"
const val CONFIG_OPT_RELEASE_TARGET_COMMITISH = "josm.release_target_commitish"

// environment variables
const val ENV_VAR_GITHUB_USER = "GITHUB_USER"
const val ENV_VAR_GITHUB_ACCESS_TOKEN = "GITHUB_ACCESS_TOKEN"
const val ENV_VAR_GITHUB_API_URL = "GITHUB_API_URL"
const val ENV_VAR_GITHUB_REPOSITORY = "GITHUB_REPOSITORY"

/**
 * Base class for tasks related to the management of github releases
 */
open class BaseGithubReleaseTask: DefaultTask() {
    @Option(
        option = "release-label",
        description = "the release label. Example: v0.0.1")
    lateinit var releaseLabel: String

    @Option(
        option = "github-user",
        description = "the github user id")
    lateinit var githubUser: String

    @Option(
        option = "github-repository",
        description = "the name of the github repository")
    lateinit var githubRepository: String

    @Option(
        option = "github-access-token",
        description = "the github access token")
    lateinit var githubAccessToken: String

    @Option(
        option = "github-api-url",
        description = "the github api url")
    lateinit var githubApiUrl: String

    @Option(
        option = "releases-config-file",
        description = "the full path to the release configuration file. Default: \$PROJECT_DIR/releases.yml")
    lateinit var releasesConfigFile: String

    @Option(
        option = "releases-target-commitish",
        description = "the target commitish for the release, i.e. 'master' or 'deploy'. Default: 'master'")
    lateinit var targetCommitish: String

    private val defaultReleasesConfigFile: File by lazy {
        File(this.project.projectDir, "releases.yml")
    }

    /**
     * the configured release config file
     */
    val configuredReleasesConfigFile: File by lazy {
        fun ensureReleaseConfigFileReadable(configFile: File) {
          if (! (configFile.isFile && configFile.exists() && configFile.canRead())) {
            throw IOException("releases configuration file <${configFile.absolutePath}> doesn't exist or can't be read")
          }
        }

        val file =
            if (::releasesConfigFile.isInitialized) {
                File(releasesConfigFile.trim())
            }
            else {
                lookupConfiguredProperty(
                    propertyName = CONFIG_OPT_RELEASES_CONFIG_FILE
                )?.trim().let {name ->
                    if (name.isNullOrEmpty()) null else File(name)
                } ?: defaultReleasesConfigFile
            }

        ensureReleaseConfigFileReadable(file)
        file
    }

    val configuredReleaseLabel: String by lazy {
        val notConfigured = GithubReleaseTaskException(
              """No release label configured. Configure it in the task, i.e.
            |   task createRelease(type: CreateGithubReleaseTask) {
            |       releaseLabel = "v1.0.0"
            |   }
            |or with the command line option --release-label""".trimMargin("|")
        )

        if (::releaseLabel.isInitialized) releaseLabel.trim()
        else throw notConfigured
    }

    val configuredGithubUser: String? by lazy {
        val notConfigured = GithubReleaseTaskException(
            """No github user name configured. Configure it in the task, i.e.
              |   task createRelease(type: CreateGithubReleaseTask) {
              |       githubUser = "my-user-name"
              |   }
              |with the command line option --github-user,
              |the project property $CONFIG_OPT_GITHUB_USER
              |or the environment variable $ENV_VAR_GITHUB_USER""".trimMargin("|")
        )

        if (::githubUser.isInitialized) {
            githubUser.trim()
        }
        else {
            lookupConfiguredProperty(
                propertyName = CONFIG_OPT_GITHUB_USER,
                envName = ENV_VAR_GITHUB_USER
            ) ?: throw notConfigured
        }
    }

    val configuredGithubAccessToken: String? by lazy {
        val notConfigured = GithubReleaseTaskException(
            """No github access token configured. Configure it in the task, i.e.
            |     task createRelease(type: CreateGithubReleaseTask) {
            |         githubAccessToken = "abcx23234..."
            |     }
            |with the command line option --github-access-token,
            |the project property $CONFIG_OPT_GITHUB_ACCESS_TOKEN
            |or the environment variable $ENV_VAR_GITHUB_ACCESS_TOKEN""".trimMargin("|")
        )

        if (::githubAccessToken.isInitialized) {
            githubAccessToken.trim()
        }
        else {
            lookupConfiguredProperty(
                propertyName =  CONFIG_OPT_GITHUB_ACCESS_TOKEN,
                envName = "GITHUB_ACCESS_TOKEN"
            ) ?: throw notConfigured
        }
      }

    val configuredGithubApiUrl: String by lazy {
        if (::githubApiUrl.isInitialized) {
            githubApiUrl.trim()
        }
        else {
            lookupConfiguredProperty(
                propertyName = CONFIG_OPT_GITHUB_API_URL,
                envName = ENV_VAR_GITHUB_API_URL
              ) ?: DEFAULT_GITHUB_API_URL
        }
      }

    val configuredTargetCommitish: String by lazy {
        if (::targetCommitish.isInitialized) {
            targetCommitish.trim()
        }
        else {
            lookupConfiguredProperty(
                propertyName = CONFIG_OPT_RELEASE_TARGET_COMMITISH
            ) ?: DEFAULT_TARGET_COMMITISH
        }
    }

    val configuredGithubRepository: String? by lazy {

        val notConfigured = GithubReleaseTaskException(
            """No github repository configured. Configure it in the task, i.e.
            |   task createRelease(type: CreateGithubReleaseTask) {
            |       githubRepository = "my-github-repo"
            |   }
            |with the command line option --github-repository,
            |the project property $CONFIG_OPT_GITHUB_REPOSITORY
            |or the environment variable $ENV_VAR_GITHUB_REPOSITORY""".trimMargin("|")
        )

        if (::githubRepository.isInitialized) {
            githubRepository.trim()
        }
        else {
            lookupConfiguredProperty(
                propertyName = CONFIG_OPT_GITHUB_REPOSITORY,
                envName = ENV_VAR_GITHUB_REPOSITORY
            ) ?: throw notConfigured
        }
    }

    @Throws(GithubReleaseTaskException::class)
    protected fun ensureValidNumericPluginVersion(release: ReleaseSpec) {
        if (release.numericPluginVersion <= 0) {
            throw GithubReleaseTaskException(
                """Illegal numeric plugin version '${release.numericPluginVersion}' for release
                    |'${release.label}'.
                    |Fix in '$configuredReleasesConfigFile'"""
                  .trimMargin("|")
            )
        }
    }

    @Throws(GithubReleaseTaskException::class)
    protected fun ensureValidNumericJsomVersion(release: ReleaseSpec) {
        if (release.numericJosmVersion <= 0) {
            throw GithubReleaseTaskException(
                """Illegal numeric josm version '${release.numericJosmVersion}' for release
                    |'$releaseLabel'.
                    |Fix in '$configuredReleasesConfigFile'"""
                  .trimMargin("|")
            )
        }
      }

    protected val githubReleaseClient: GithubReleasesClient by lazy {
        GithubReleasesClient(
            repository = configuredGithubRepository,
            user = configuredGithubUser,
            accessToken = configuredGithubAccessToken,
            apiUrl = configuredGithubApiUrl
        )
    }

    private fun lookupConfiguredProperty(propertyName: String, envName: String? = null): String? {
        val prop = this.project.findProperty(propertyName)?.toString()?.trim()

        return if (!prop.isNullOrEmpty()) { prop }
        else {
            envName?.let {
                System.getenv(envName)?.trim().let {value->
                  if (value.isNullOrEmpty()) null else value
                }
            }
          }
      }
}

/**
 * Task to create a github relase using the github API.
 *
 */
open class CreateGithubReleaseTask : BaseGithubReleaseTask() {

    @TaskAction
    fun createGithubRelease() {
        val releaseConfigFile = configuredReleasesConfigFile
        val releaseLabel = configuredReleaseLabel
        val releaseConfig = ReleasesSpec.load(releaseConfigFile)

        val release = (releaseConfig?.releases?.find {it.label == releaseLabel}) ?: throw GithubReleaseTaskException(
            """The releases config file '$releaseConfigFile' doesn't include a release
                |with release label '$releaseLabel yet.
                |Add and configure a release with this label in '$releaseConfigFile' and rerun."""
              .trimMargin("|")
        )
        ensureValidNumericPluginVersion(release)
        ensureValidNumericJsomVersion(release)
        val client = githubReleaseClient

        val remoteRelease = client.getReleases()?.find {it["label"] == releaseLabel}

        remoteRelease?.let {
            throw GithubReleaseTaskException(
                "Release with release label '${releaseLabel}' already exists on the github server"
            )
        }

        try {
            val newRelease = client.createRelease(
                tagName = releaseLabel,
                targetCommitish = configuredTargetCommitish,
                name = release.name ?: releaseLabel,
                body = release.description)
            println("Success: new release '${releaseLabel}' created in github repository")
        } catch(e: Throwable) {
           throw GithubReleaseTaskException(e.message ?: "", e)
        }
      }
}

open class PublishToGithubReleaseTask : BaseGithubReleaseTask() {
    companion object {
        private const val CMDLINE_OPT_LOCAL_JAR_PATH = "local-jar-path"
        private const val CMDLINE_OPT_REMOTE_JAR_NAME = "remote-jar-name"
        private const val CMDLINE_OPT_UPDATE_LATEST = "update-latest"
    }

    @Option(
        option = CMDLINE_OPT_LOCAL_JAR_PATH,
        description = "the local path to the jar which should bei uploaded.\n"
          + "Default: the path of the jar built in the project")
    lateinit var localJarPath: String

    @Option(
        option = CMDLINE_OPT_REMOTE_JAR_NAME,
        description = "the name of the jar after uploading.\n"
          + "Default: the name of the local jar")
    lateinit var remoteJarName: String

    @Option(
        option = CMDLINE_OPT_UPDATE_LATEST,
        description = "indicates whether the asset is also updated to the latest release.\n"
            + "The label of the latest release is configured in 'releases.yml' and defaults\n"
            + "to 'latest'.\n"
            + "Default: false, if missing or if illegal value"
    )
    lateinit var updateLatest: Any

    private val jarArchivePath: String?  by lazy {
        project.tasks.withType(Jar::class.java).getByName("jar")?.archivePath?.absolutePath
    }

    private val jarArchiveName: String?  by lazy {
        project.tasks.withType(Jar::class.java).getByName("jar")?.archivePath?.name
    }

    val configuredLocalJarPath: String? by lazy {
        val notConfigured = GithubReleaseTaskException(
            """Missing configuration for local jar to publish as release asset. Configure it n the task, i.e.
            |   task publishRelease(type: PublishToGithubReleaseTask) {
            |       localJarPath = "/path/to/the/jar/to/publish.jar"
            |   }
            |or with the command line option --$CMDLINE_OPT_LOCAL_JAR_PATH
            """.trimMargin("|")
        )

        if (::localJarPath.isInitialized) localJarPath
        else jarArchivePath ?: throw notConfigured
    }

    val configuredRemoteJarName: String? by lazy {
        val notConfigured = GithubReleaseTaskException(
            """Missing configuration for remote jar name. Configure it in the task, i.e.
            |   task publishRelease(type: PublishToGithubReleaseTask) {
            |       remoteJarName = "my-josm-plugin.jar"
            |   }
            |or with the command line option --$CMDLINE_OPT_REMOTE_JAR_NAME
            """.trimMargin("|")
        )

        if (::remoteJarName.isInitialized && ! remoteJarName.trim().isNullOrBlank())
          remoteJarName.trim()
        else jarArchiveName ?: throw notConfigured
    }

    val configuredUpdateLatest: Boolean by lazy {
        fun fromString(value: String): Boolean =
            when(value.toLowerCase().trim()) {
                "true", "1", "yes", "y" -> true
                else -> false
            }

        if (!::updateLatest.isInitialized) false
        else
            when(::updateLatest.get()) {
                is Boolean -> ::updateLatest.get() as Boolean
                else -> fromString(::updateLatest.get().toString())
            }
    }

    /**
     * true, if this file can be uploaded to a github release, i.e. if it
     * is an existing, readable, locally available file
     */
    internal val File.canUpload: Boolean
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
     * throws an exception if this file isn't a valid jar file with a manifest including
     *   * an attribute `Plugin-Version`
     *   * an attribute `Plugin-Mainversion`
     *
     * Both must match the respecitive attributes in [release].
     *
     * @exception GithubReleaseTaskException thrown, if this file isn't consistent with
     *   the release spec
     */
    @Throws(GithubReleaseTaskException::class)
    internal fun File.ensureConsistentWithReleaseSpec(release: ReleaseSpec) {
        val jarFile = try {
            JarFile(this)
        } catch (e: IOException) {
            null
        } ?: throw GithubReleaseTaskException(
            """The file ${this.absolutePath} isn't a valid jar file."""
        )
        val manifest = jarFile.manifest
        manifest ?: throw GithubReleaseTaskException(
          "The jar file ${this.absolutePath} doesn't include a MANIFEST file"
        )
        val pluginVersion = try {
            manifest.mainAttributes.getValue("Plugin-Version")?.trim()?.toInt()
        } catch (e: Throwable) {
            null
        } ?: throw GithubReleaseTaskException(
            """The jar file '${this.absolutePath}' does either not include an attribute
                | 'Plugin-Version' or its value isn't a positive int"""".trimMargin("|")
        )
        if (pluginVersion != release.numericPluginVersion) {
            throw GithubReleaseTaskException(
                """The numeric plugin version in the in the MANIFEST of the jar file
                  |'${this.absolutePath}' doesn't match with numeric plugin version of the release
                  | Numeric plugin version for the release: ${release.numericPluginVersion}
                  | Numeric plugin version in the MANIFEST: ${pluginVersion}
                  """.trimMargin("|")
            )
        }

        val josmVersion = try {
            manifest.mainAttributes.getValue("Plugin-Mainversion")?.trim()?.toInt()
        } catch (e: Throwable) {
            null
        } ?: throw GithubReleaseTaskException(
            """The jar file '${this.absolutePath}' does either not include an attribute
                | 'Plugin-Main-Version' or its value isn't a positive int"""".trimMargin("|")
        )
        if (josmVersion != release.numericJosmVersion) {
            throw GithubReleaseTaskException(
                """The numeric JOSM version in the in the MANIFEST of the jar file
                  |'${this.absolutePath}' doesn't match with the numeric JOSM version of the release
                  | Numeric JOSM version for the release: ${release.numericJosmVersion}
                  | Numeric JOSM version in the MANIFEST: ${josmVersion}
                  """.trimMargin("|")
            )
        }
    }

    @TaskAction
    fun publishToGithubRelease() {
        val localJarPath = configuredLocalJarPath
        val remoteJarName = configuredRemoteJarName

        val releaseLabel = configuredReleaseLabel
        val client = githubReleaseClient

        val releaseConfig = ReleasesSpec.load(configuredReleasesConfigFile)

        val localRelease = (releaseConfig?.releases?.find {it.label == releaseLabel})
            ?: throw GithubReleaseTaskException(
              """The releases config file '$configuredReleasesConfigFile' doesn't include a release
                  |with release label '$releaseLabel yet.
                  |Add and configure a release with this label in '$configuredReleasesConfigFile' and rerun."""
                .trimMargin("|")
            )
        ensureValidNumericPluginVersion(localRelease)
        ensureValidNumericJsomVersion(localRelease)
        val remoteReleases = client.getReleases()

        val remoteRelease = remoteReleases?.find {it["tag_name"] == releaseLabel}
        remoteRelease ?: throw GithubReleaseTaskException(
            "Release with tag_name '${releaseLabel}' doesn't exist on the github server"
        )

        val releaseId = remoteRelease["id"].toString().toInt()
        val localFile = File(configuredLocalJarPath)
        if (!localFile.canUpload) {
            throw GithubReleaseTaskException(
              """Local jar file '$configuredLocalJarPath' doesn't exist
                |or isn't readable. Can't upload it as release artefact.""".trimMargin("|")
            )
        }
        localFile.ensureConsistentWithReleaseSpec(localRelease)

        println("Uploading '${localFile.name}' to release '$releaseLabel' at ${client.apiUrl}")
        client.uploadReleaseAsset(
            releaseId = releaseId,
            name = configuredRemoteJarName,
            contentType = MEDIA_TYPE_JAR,
            file = localFile
        )

        if (configuredUpdateLatest) {
            val latestReleaseLabel = releaseConfig.latest
            val notFound = GithubReleaseTaskException(
                """remote release with label '$latestReleaseLabel' not found"""
            )
            val latestRelease = remoteReleases?.find {it["tag_name"] == latestReleaseLabel}
                ?: throw notFound
            val releaseId = latestRelease["id"].toString().toInt()
            println("Uploading '${localFile.name}' to release '$latestReleaseLabel' at ${client.apiUrl}")
            client.uploadReleaseAsset(
                releaseId = releaseId,
                name = configuredRemoteJarName,
                contentType = MEDIA_TYPE_JAR,
                file = localFile
            )
        }
    }
}
