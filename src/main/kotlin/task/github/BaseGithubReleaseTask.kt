package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.openstreetmap.josm.gradle.plugin.config.GithubConfig
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.util.josm

private const val CMDLINE_OPT_RELEASE_LABEL = "release-label"
private const val CMDLINE_OPT_TARGET_COMMITISH = "target-commitish"

/**
 * Base class for tasks related to the management of github releases
 *
 * Note: This is currently in beta stage, so expect sudden changes to this class anytime.
 */
abstract class BaseGithubReleaseTask: DefaultTask() {

  init {
    outputs.upToDateWhen { false } // never consider this up-to-date, since this task interacts with a remote server, so the outputs can't be checked easily
  }

  private val releaseLabelNotConfigured by lazy {
    GithubReleaseException(
      """Release label not configured or blank.
        |Configure it in the task, i.e.
        |   ${this.name} {
        |       releaseLabel = "v1.0.0"
        |   }
        |or set the project property 'version', i.e.
        |   version = "v1.0.0"
        |or use the command line option --$CMDLINE_OPT_RELEASE_LABEL"""
        .trimMargin("|")
    )
  }

  @get:Internal
  @Option(
    option = CMDLINE_OPT_RELEASE_LABEL,
    description = "the release label. Example: v0.0.1")
  var releaseLabel: String? = null

  @get:Internal
  @Option(
    option = CMDLINE_OPT_TARGET_COMMITISH,
    description = "the target commitish for the release, e.g. 'master' "
      + "or 'deploy'. Default: '${GithubConfig.DEFAULT_TARGET_COMMITTISH}' (if not configured differently)")
  var targetCommitish: String? = null

  @get:Internal
  val configuredReleaseLabel: String by lazy {
    releaseLabel.takeIf { !it.isNullOrBlank() }
      ?: project.version.toString().takeIf { !it.isBlank() && it != Project.DEFAULT_VERSION }
      ?: throw releaseLabelNotConfigured
  }

  @get:Internal
  val configuredTargetCommitish: String by lazy {
    targetCommitish.takeIf { !it.isNullOrBlank() } ?: project.extensions.josm.github.targetCommitish
  }
}
