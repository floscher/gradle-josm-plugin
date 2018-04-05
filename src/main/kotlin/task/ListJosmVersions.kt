package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets

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

  private fun readTestedVersion() = readNumericVersionAsString(URL("$BASE_URL/tested"))
  private fun readLatestVersion() = readNumericVersionAsString(URL("$BASE_URL/latest"))

  private fun readNumericVersionAsString(url: URL) = try {
    readNumericVersion(url).toString()
  } catch (e: NumberFormatException) {
    "‹invalid version format›"
  } catch (e: IOException) {
    "‹connection error›"
  }

  /**
   * @throws IOException
   * @throws NumberFormatException
   */
  private fun readNumericVersion(url: URL): Int {
    url.openStream().use {
      return BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).readLine().toInt()
    }
  }
}

