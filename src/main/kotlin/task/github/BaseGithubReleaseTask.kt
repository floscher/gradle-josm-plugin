package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.josm

private const val CMDLINE_OPT_RELEASE_LABEL = "release-label"
private const val CMDLINE_OPT_TARGET_COMMITISH = "target-commitish"

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
    val notConfigured = GithubReleaseException(
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
    val tmpCommitish = targetCommitish
    if (tmpCommitish != null && !tmpCommitish.isNullOrBlank()) {
      tmpCommitish
    } else {
      project.extensions.josm.github.targetCommitish
    }
  }
}
