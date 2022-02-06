/**
 * Extends the `jar` task to write the JOSM specific manifest
 */
package org.openstreetmap.josm.gradle.plugin.task

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest.Attribute
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleaseSpec
import org.openstreetmap.josm.gradle.plugin.github.onlyFallbackVersions
import org.openstreetmap.josm.gradle.plugin.util.josm

  /**
   * Builds the map of download URLs
   */
  internal fun buildMapOfGitHubDownloadLinks(project: Project) : Map<String,String> {

    fun JsonObject.downloadUrl() : String? {
      val assets = this["assets"] as? JsonArray<*>
      return assets
        ?.mapNotNull {
          (it as? JsonObject)?.get("browser_download_url")?.toString()
        }
        ?.find { it.endsWith(".jar") }
    }

    val specs = ReleaseSpec.loadListFrom(project.extensions.josm.github.releasesConfig.inputStream())
    val remoteReleases = try {
      val client = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.apiUrl)
      client.getReleases()
    } catch(e: GithubReleaseException) {
      project.logger.warn("""
          Failed to retrieve list of remote releases.
          Reason: ${e.message}
          List of remote releases not available.
          Can't create map of download links for remote releases."""
        .trimIndent()
      )
      return emptyMap()
    }

    return specs.onlyFallbackVersions()
      .fold(initial=mutableMapOf()) fold@{links, release ->
        val remoteRelease = remoteReleases.find { it["tag_name"] == release.label}
          ?: run {
            project.logger.warn(
              "Could not find a remote release for the release label " +
                "'${release.label}'. No download link included in the " +
                "MANIFEST file for JOSM release ${release.minJosmVersion}"
            )
            return@fold links
          }
        val downloadUrl = remoteRelease.downloadUrl() ?: run {
          project.logger.warn(
            "Could not find a jar download url for the remote release with " +
              "label '${release.label}'. No download link included in the " +
              "MANIFEST file for JOSM release ${release.minJosmVersion}"
          )
          return@fold links
        }
        val value = "${release.label};$downloadUrl"

        links[Attribute.pluginDownloadLinkKey(release.minJosmVersion)] = value
        links
      }
  }
