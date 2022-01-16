/**
 * Extends the `jar` task to write the JOSM specific manifest
 */
package org.openstreetmap.josm.gradle.plugin.task

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import groovy.lang.GroovySystem
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest.Attribute
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleaseSpec
import org.openstreetmap.josm.gradle.plugin.github.onlyFallbackVersions
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.util.GregorianCalendar

private data class RequiredAttribute(val key: Attribute, val value: String?, val description: String)

fun Jar.addJosmManifest(i18nCompileTask: TaskProvider<out CompileToLang>?) {
  if (i18nCompileTask != null) {
    dependsOn(i18nCompileTask)
  }
  doFirst {
    manifest.attributes(buildAttributeMap(
      project,
      project.extensions.josm.manifest,
      i18nCompileTask?.get(),
      project.extensions.josm.manifest.description
    ))
  }
}

private fun buildAttributeMap(
  project: Project,
  josmManifest: JosmManifest,
  i18nCompileTask: CompileToLang?,
  baseLanguageDescription: String?
): Map<String, String> = getRequiredManifestAttributes(josmManifest)
  .plus(
    arrayOf(
      // Default attributes (always set)
      Attribute.CLASSPATH to josmManifest.classpath.joinToString(" "),
      Attribute.CREATED_BY to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
      Attribute.GRADLE_VERSION to project.gradle.gradleVersion,
      Attribute.GROOVY_VERSION to GroovySystem.getVersion(),
      Attribute.PLUGIN_DATE to String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", GregorianCalendar()),
      Attribute.PLUGIN_VERSION to project.version.toString(),
      Attribute.PLUGIN_EARLY to josmManifest.loadEarly.toString(),
      Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME to josmManifest.canLoadAtRuntime.toString(),

      // Optional attributes (can be null)
      Attribute.AUTHOR to josmManifest.author,
      Attribute.PLUGIN_ICON to josmManifest.iconPath,
      Attribute.PLUGIN_LOAD_PRIORITY to josmManifest.loadPriority?.toString(),
      Attribute.PLUGIN_MIN_JAVA_VERSION to josmManifest.minJavaVersion?.toString(),
      Attribute.PLUGIN_PLATFORM to josmManifest.platform?.toString(),
      Attribute.PLUGIN_PROVIDES to josmManifest.provides,
      Attribute.PLUGIN_WEBSITE to josmManifest.website?.toString(),
      Attribute.PLUGIN_DEPENDENCIES to josmManifest.pluginDependencies.getOrElse(emptySet()).ifEmpty { null }?.joinToString(";")
    )
  )
    // Convert attributes keys to strings
    .mapKeys { it.key.manifestKey }
    // Add download links to older GitHub releases
    .plus(
      if (josmManifest.includeLinksToGithubReleases) {
        buildMapOfGitHubDownloadLinks(project)
      } else emptyMap()
    )
    // Add manually specified links to older versions of the plugin
    .plus(
      josmManifest.oldVersionDownloadLinks
        .map { Attribute.pluginDownloadLinkKey(it.minJosmVersion) to "${it.pluginVersion};${it.downloadURL}" }
    )
    // Add translations for the plugin description from the i18n files
    .plus(
      if (baseLanguageDescription == null || i18nCompileTask == null) {
        listOf()
      } else {
        i18nCompileTask.translations.mapNotNull { (language, t) ->
          t.entries
            .firstOrNull { it.key == MsgId(MsgStr(baseLanguageDescription)) }
            ?.let { Attribute.pluginDescriptionKey(language) to it.value.strings.joinToString("\n") }
        }
      }
    )
    // Filter out unset attributes
    .mapNotNull { att -> att.value?.let { att.key to it }  }.toMap()
    // Log the attributes
    .apply {
      if (project.logger.isInfoEnabled) {
        project.logger.info("The following lines will be added to the manifest of the plugin *.jar file:")
        for (e in this.toSortedMap()) {
          project.logger.info("  ${e.key}: ${e.value}")
        }
      }
    }

/**
 * @return a list of required manifest attributes
 * @throws GradleException if any required manifest attribute is not set
 */
private fun getRequiredManifestAttributes(josmManifest: JosmManifest): Map<Attribute, String?> = listOfNotNull(
  RequiredAttribute(
    Attribute.PLUGIN_MIN_JOSM_VERSION,
    josmManifest.minJosmVersion,
    "the minimum JOSM version your plugin is compatible with"
  ),
  RequiredAttribute(
    Attribute.PLUGIN_MAIN_CLASS,
    josmManifest.mainClass,
    "the full name of the main class of your plugin"
  ),
  RequiredAttribute(
    Attribute.PLUGIN_DESCRIPTION,
    josmManifest.description,
    "the textual description of your plugin"
  ),
  if (josmManifest.provides == null) null else {
    RequiredAttribute(
      Attribute.PLUGIN_PLATFORM,
      josmManifest.platform?.toString(),
      "the platform for which this plugin is written (must be either Osx, Windows or Unixoid)"
    )
  },
  if (josmManifest.platform == null) null else {
    RequiredAttribute(
      Attribute.PLUGIN_PROVIDES,
      josmManifest.provides,
      "the name of the virtual plugin for which this is an implementation"
    )
  }
)
  .apply {
    val missingRequiredFields = filter { it.value == null }
    if (missingRequiredFields.isNotEmpty()) {
      throw GradleException(
        """
        |The JOSM plugin misses these required configuration options:
        |${missingRequiredFields.joinToString("\n") { " * ${it.description}" }}
        |You can add these lines to your `gradle.properties` file in order to fix this:
        |${missingRequiredFields.joinToString { " * ${it.key.propertiesKey} = ‹${it.description}›" }}
        |Or alternatively add the equivalent lines to the `josm.manifest {}` block in your `build.gradle(.kts)` file.
        """.trimMargin()
      )
    }
  }
  .map { it.key to it.value!! }
  .toMap()

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
