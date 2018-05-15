package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.ghreleases.DEFAULT_GITHUB_API_URL
import org.openstreetmap.josm.gradle.plugin.ghreleases.GithubReleasesClient
import java.io.File
import java.io.IOException

class GithubReleaseTaskException(override var message: String, override var cause: Throwable?)
  : Exception(message, cause) {
  constructor(message: String) : this(message, null) {
  }
}

const val DEFAULT_TARGET_COMMITISH = "master"

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

  val configuredReleasesConfigFile: File by lazy {
    fun ensureReleaseConfigFileReadable(configFile: File) {
      if (! (configFile.isFile && configFile.exists() && configFile.canRead())) {
        throw IOException("releases configuration file <${configFile.absolutePath}> doesn't exist or can't be read")
      }
    }

    val f = if (::releasesConfigFile.isInitialized) File(releasesConfigFile.trim())
      else {
        val fileName= lookupConfiguredProperty(
          propertyName = "org.openstreetmap.josm.gradle.plugin.releases_config_file"
        )?.trim()

        if (fileName.isNullOrEmpty()) null else File(fileName)
      } ?: defaultReleasesConfigFile

    ensureReleaseConfigFileReadable(f)
    f
  }

  val configuredReleaseLabel: String by lazy {
      if (::releaseLabel.isInitialized) releaseLabel.trim()
      else throw GithubReleaseTaskException(
        """No release label configured. Configure it in the task
      |   task myTask(...) {
      |       releaseLabel = "v1.0.0"
      |   }
      |or with the command line option --release-label""".trimMargin("|")
      )
    }

  val configuredGithubUser: String? by lazy {
    if (::githubUser.isInitialized) githubUser.trim()
    else lookupConfiguredProperty(
        propertyName = "org.openstreetmap.josm.gradle.plugin.github_user",
        envName = "GITHUB_USER"
      ) ?: throw GithubReleaseTaskException(
        """No github user name configured. Configure it in the task
        |   task myTask(...) {
        |       githubUser = "my-user-name"
        |   }
        |with the command line option --github-user,
        |the project property org.openstreetmap.josm.gradle.plugin.github_user
        |or the environment variable GITHUB_USER""".trimMargin("|")
      )
  }

  val configuredGithubAccessToken: String? by lazy {
    if (::githubAccessToken.isInitialized) githubAccessToken.trim()
    else lookupConfiguredProperty(
      propertyName =  "org.openstreetmap.josm.gradle.plugin.github_access_token",
      envName = "GITHUB_ACCESS_TOKEN"
    ) ?: throw GithubReleaseTaskException(
      """No github access token configured. Configure it in the task
      |   task myTask(...) {
      |       githubAccessToken = "abcx23234..."
      |   }
      |with the command line option --github-access-token,
      |the project property org.openstreetmap.josm.gradle.plugin.github_access_token
      |or the environment variable GITHUB_ACCESS_TOKEN""".trimMargin("|")
    )
  }

  val configuredGithubApiUrl: String by lazy {
    if (::githubApiUrl.isInitialized) githubApiUrl.trim()
    else lookupConfiguredProperty(
        propertyName = "org.openstreetmap.josm.gradle.plugin.github_api_url",
        envName = "GITHUB_API_URL"
      ) ?: DEFAULT_GITHUB_API_URL
  }
  val configuredTargetCommitish: String by lazy {
    if (::targetCommitish.isInitialized) targetCommitish.trim()
    else lookupConfiguredProperty(
        propertyName = "org.openstreetmap.josm.gradle.plugin.release_target_commitish"
      ) ?: DEFAULT_TARGET_COMMITISH
  }
  val configuredGithubRepository: String? by lazy {
    if (::githubRepository.isInitialized) githubRepository.trim()
    else lookupConfiguredProperty(
      propertyName = "org.openstreetmap.josm.gradle.plugin.github_repository",
      envName = "GITHUB_REPOSITORY"
    ) ?: throw GithubReleaseTaskException(
      """No github repository configured. Configure it in the task
      |   task myTask(...) {
      |       githubRepository = "my-github-repo"
      |   }
      |with the command line option --github-repository,
      |the project property org.openstreetmap.josm.gradle.plugin.github_repository
      |or the environment variable GITHUB_REPOSITORY""".trimMargin("|")
    )
  }

  private fun lookupConfiguredProperty(propertyName: String, envName: String? = null): String? {
    val prop = this.project.findProperty(propertyName)?.toString()?.trim()

    return if (!prop.isNullOrEmpty()) prop
      else if (envName != null) {
        val envValue: String? = System.getenv(envName)?.trim()
        if (envValue.isNullOrEmpty()) null else envValue
      }
      else null
  }
}

open class CreateGithubReleaseTask : BaseGithubReleaseTask() {

  @TaskAction
  fun createGithubRelease() {
    val releaseConfigFile = configuredReleasesConfigFile
    val releaseLabel = configuredReleaseLabel
    val releaseConfig = Releases.load(releaseConfigFile)

    val release = (releaseConfig.releases?.find {it.label == releaseLabel}) ?: throw GithubReleaseTaskException(
      """The releases config file '$releaseConfigFile' doesn't include a release
          |with release label '$releaseLabel yet.
          |Add and configure a release with this label in '$releaseConfigFile' and rerun."""
        .trimMargin("|")
    )
    if (release.numericPluginVersion <= 0) {
      throw GithubReleaseTaskException(
        """Illegal numeric plugin version '${release.numericPluginVersion}' for release
            |'$releaseLabel'.
            |Fix in '$releaseConfigFile' and rerun"""
          .trimMargin("|")
      )
    }
    if (release.numericJosmVersion <= 0) {
      throw GithubReleaseTaskException(
        """Illegal numeric josm version '${release.numericJosmVersion}' for release
            |'$releaseLabel'.
            |Fix in '$releaseConfigFile' and rerun"""
          .trimMargin("|")
      )
    }

    val client = GithubReleasesClient(
      repository = configuredGithubRepository,
      user=configuredGithubUser,
      accessToken = configuredGithubAccessToken,
      apiUrl = configuredGithubApiUrl)

    val remoteRelease = client.getReleases()?.find {it["label"] == releaseLabel}

    remoteRelease?.let {
      println("remote release: ${remoteRelease}")
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
        println("Success: new release '${releaseLabel}' created on github repository")
    } catch(e: Throwable) {
      throw GithubReleaseTaskException(e.message ?: "", e)
    }
  }
}
