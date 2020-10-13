package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.util.Urls
import java.io.IOException
import java.net.URL

/**
 * Task for showing the current `latest` and `tested` JOSM versions
 */
open class ListJosmVersions : DefaultTask() {

  companion object {
    const val ERROR_CODE_CONNECTION = -1
    const val ERROR_CODE_WRONG_FORMAT = -2
  }

  init {
    group = "JOSM"
    description = "Shows the current 'latest' and 'tested' JOSM versions"
  }

  @TaskAction
  fun action() {
    logger.lifecycle("# Available JOSM versions ($ERROR_CODE_CONNECTION means connection error, $ERROR_CODE_WRONG_FORMAT means something that was not a positive integer was read)")
    logger.lifecycle("tested=${ readNumericVersion(Urls.MainJosmWebsite.VERSION_NUMBER_TESTED) }")
    logger.lifecycle("latest=${ readNumericVersion(Urls.MainJosmWebsite.VERSION_NUMBER_LATEST) }")
    logger.info("(There might be even later versions available, 'latest' refers to the latest nightly build)")
  }

  private fun readNumericVersion(url: URL): Int = try {
    url.readText().toIntOrNull()?.takeIf { it > 0 } ?: ERROR_CODE_WRONG_FORMAT
  } catch (e: IOException) {
    ERROR_CODE_CONNECTION
  }
}

