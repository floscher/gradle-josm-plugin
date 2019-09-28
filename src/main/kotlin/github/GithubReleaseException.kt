package org.openstreetmap.josm.gradle.plugin.github

import okhttp3.Response

/**
 * An exception that occured in a task related to publishing a release to GitHub releases.
 * @param [message] the message of the exception
 * @param [cause] a [Throwable] that caused this exception to be thrown
 */
class GithubReleaseException(message: String?, cause: Throwable?): Exception(message, cause) {
  constructor(cause: Throwable) : this(null, cause)
  constructor(message: String) : this(message, null)
  constructor(response: Response) : this(
    "Unexpected response from GitHub API: $response.\n" +
    "Response body:\n${response.body?.string() ?: ""}"
  )
}
