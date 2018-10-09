package org.openstreetmap.josm.gradle.plugin.github

import okhttp3.Response

class GithubReleaseException(message: String?, cause: Throwable?): Exception(message, cause) {

  constructor(cause: Throwable) : this(null, cause)
  constructor(message: String) : this(message, null)
  constructor(response: Response) : this(
    "Unexpected response from GitHub API: $response.\n" +
    "Response body:\n${response.body()?.string() ?: ""}"
  )

  companion object {
    fun remoteReleaseDoesntExist(releaseLabel: String)
      : GithubReleaseException {
      val msg = """Remote release with label '$releaseLabel' doesn't
        |exist on the GitHub server.
        |Can't upload release jar to the release '$releaseLabel.
        |Create release '$releaseLabel' first, i.e.
        |  ./gradlew createGithubRelease --release-label $releaseLabel
        """.trimMargin("|")
      return GithubReleaseException(msg)
    }

    fun remotePickupReleaseDoesntExit(releaseLabel: String)
      : GithubReleaseException {
      val msg = """Remote pickup release with label '$releaseLabel'
        |doesn't exist on the GitHub server.
        |Can't upload release jar to the pickup release '$releaseLabel'.
        |Create pickup release first, i.e.
        |   ./gradlew createPickupRelease
        |"""
        .trimMargin("|")
      return GithubReleaseException(msg)
    }
  }
}
