package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Task for showing the current `latest` and `tested` JOSM versions
 */
open class ListJosmVersions : DefaultTask() {
  private val BASE_URL = "https://josm.openstreetmap.de"

  init {
    group = "JOSM"
    description = "Shows the current 'latest' and 'tested' JOSM versions"
    doFirst {
      logger.lifecycle("# Available JOSM versions")
      logger.lifecycle("tested=${readTestedVersion()}")
      logger.lifecycle("latest=${readLatestVersion()}")
      logger.info("(There might be even later versions available, 'latest' refers to the latest nightly build)")
    }
  }

  private fun readTestedVersion() = readNumericVersion(URL("$BASE_URL/tested"))
  private fun readLatestVersion() = readNumericVersion(URL("$BASE_URL/latest"))

  private fun readNumericVersion(url: URL): Int {
    url.openStream().use {
      return BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).readLine().toInt()
    }
  }
}

