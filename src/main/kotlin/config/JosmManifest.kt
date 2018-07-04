package org.openstreetmap.josm.gradle.plugin.config

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import groovy.lang.GroovySystem
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.ghreleases.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.josm
import org.openstreetmap.josm.gradle.plugin.task.LangCompile
import org.openstreetmap.josm.gradle.plugin.task.ReleasesSpec
import org.openstreetmap.josm.gradle.plugin.task.configuredGithubAccessToken
import org.openstreetmap.josm.gradle.plugin.task.configuredGithubApiUrl
import org.openstreetmap.josm.gradle.plugin.task.configuredGithubRepository
import org.openstreetmap.josm.gradle.plugin.task.configuredGithubUser
import org.openstreetmap.josm.gradle.plugin.task.configuredReleasesConfigFile
import java.io.File
import java.net.URL
import java.util.GregorianCalendar

/**
 * The info that will be written into the manifest file of the plugin *.jar
 *
 * Most of the attributes are corresponding to the ones listed in
 * [the documentation for developing JOSM plugins](https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins#ThemanifestfileforaJOSMplugin).
 * But note that some of them have been renamed a bit to better express what each attribute is good for.
 *
 * The properties used by the Ant build (e.g. `plugin.author` or `plugin.description`)
 * are automatically picked up by both Ant and Gradle, if you define them in your
 * `gradle.properties` file and load them into your `build.xml` file using the following snippet:
 *
 * &#60;!-- edit the properties of this plugin in the file `gradle.properties` -->
 * &#60;property file="${basedir}/gradle.properties"/>
 *
 *
 * The detailed documentation for each field below tells you, which one corresponds
 * to which Ant/Gradle property and to which attribute in the `MANIFEST.MF` file.
 *
 * For a successful build you'll have to at least set the following properties in this class:
 * [mainClass], [description], [minJosmVersion]
 * Your project also has to set a value for [Project.getVersion()]
 */
class JosmManifest(private val project: Project) {

  /**
   * The author of the plugin.
   *
   * **Default value:** the value of property `plugin.author` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Author`
   */
  var author: String? = project.findProperty("plugin.author")?.toString()

  /**
   * Determines if the plugin needs a restart after installation. `true` if no restart is required, `false` otherwise.
   *
   * **Default value:** the value of property `plugin.canloadatruntime` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Canloadatruntime`
   */
  var canLoadAtRuntime: Boolean = project.findProperty("plugin.canloadatruntime")?.toString()?.toBoolean() ?: false

  /**
   * The description of what the plugin does.
   *
   * **Default value:** the value of property `plugin.description` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Description`
   */
  var description: String? = project.findProperty("plugin.description")?.toString()

  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via [Class.getResource](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResource(java.lang.String)).
   *
   * **Default value:** the value of property `plugin.icon` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Icon`
   */
  var iconPath: String? = project.findProperty("plugin.icon")?.toString()

  /**
   * The task that processes the *.lang files for translations. This is set automatically and you normally don't have
   * to change it.
   */
  var langCompileTask: LangCompile? = null

  /**
   * This can be set to `true`, when the plugin should load before the GUI classes of JOSM.
   *
   * **Default value:** The value of property `plugin.early` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Early`
   */
  var loadEarly: Boolean = project.findProperty("plugin.early")?.toString()?.toBoolean() ?: false

  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to `null`.
   *
   * **Default value:** The integer value of property `plugin.stage` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Stage`
   */
  var loadPriority: Int? = project.findProperty("plugin.stage")?.toString()?.toIntOrNull()

  /**
   * The full name of the main class of the plugin.
   *
   * **Default value:** the value of property `plugin.class` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Class`
   */
  var mainClass: String? = project.findProperty("plugin.class")?.toString()

  /**
   * The minimum JOSM version with which the plugin is compatible.
   *
   * **Default:** the value of property `plugin.main.version` or `null` if that property is not set.
   * **Influenced MANIFEST.MF attribute:** `Plugin-Mainversion`
   */
  var minJosmVersion: String? = project.findProperty("plugin.main.version")?.toString()

  /**
   * If true, load the old version download links from GitHub releases.
   */
  var includeLinksToGithubReleases: Boolean = false

  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * **Default:** the value of property `plugin.requires` split at every semicolon (do not rely on the order, as it is not necessarily maintained) or `null` if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>Plugin-Requires
   */
  val pluginDependencies: MutableSet<String> = mutableSetOf()

  init {
    // Fill the map containing the plugin dependencies
    val requirements: String? = project.findProperty("plugin.requires")?.toString()
    if (requirements != null) {
      val dependencyArray: List<String> = requirements.split(';')
      for (dependency in dependencyArray) {
        if (dependency.isNotBlank()) {
          pluginDependencies.add(dependency.trim())
        }
      }
    }
  }

  /**
   * Contains the {@link #description} translated to languages other than English.
   *
   */
  private val translatedDescriptions: MutableMap<String, String> = mutableMapOf()

  /**
   * A URL pointing to a web resource describing the plugin.
   *
   * **Default:** The value of property `plugin.link` as URL (might error out with a [MalformedURLException](https://docs.oracle.com/javase/8/docs/api/java/net/MalformedURLException.html) on malformed URLs), or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Link`
   */
  var website: URL? = if (project.hasProperty("plugin.link")) URL(project.findProperty("plugin.link").toString()) else null

  /**
   * For compatibility with older JOSM versions, that are not supported by the current version of the plugin.
   * This field contains URLs where versions of the plugin can be downloaded, which are compatible with certain older JOSM versions.

   * See [oldVersionDownloadLink()] on how to add entries to this attribute.
   * The URL value points to a location where the plugin can be downloaded from and the integer key denotes the minimum JOSM version that the plugin at that location is compatible with.
   */
  private val oldVersionDownloadLinks: MutableSet<PluginDownloadLink> = mutableSetOf()

  private inner class PluginDownloadLink(val minJosmVersion: Int, val pluginVersion: String, val downloadURL: URL)

  /**
   * Add a link to an earlier release of the plugin, that is compatible with JOSM versions, with which the current version is no longer compatible.
   *
   * **Influenced MANIFEST.MF attribute:** `([1-9][0-9]*)_Plugin-Url` (Any attribute that matches this [RegEx](https://en.wikipedia.org/wiki/Regular_expression). The number in the beginning is determined by the parameter `minJosmVersion`.)
   *
   * @param minJosmVersion the minimum JOSM version with which the linked plugin is compatible
   * @param pluginVersion the version number of the linked plugin
   * @param downloadURL the URL where the linked plugin can be downloaded from
   */
  public fun oldVersionDownloadLink(minJosmVersion: Int, pluginVersion: String, downloadURL: URL) {
    oldVersionDownloadLinks.add(PluginDownloadLink(minJosmVersion, pluginVersion, downloadURL))
  }

  /**
   * Logs an error message when the parameter `checkResult` is true.
   * @param checkResult a boolean value that is true when the field in question is missing
   * @param fieldDescription a textual description of the field (e.g. "the version of your plugin")
   * @param requiredValue the property which needs to be set in order to correct for this error (e.g. "josm.manifest.requiredValue = ‹some value›")
   * @return always returns the parameter `checkResult`
   */
  private fun isRequiredFieldMissing(checkResult: Boolean, fieldDescription: String, requiredValue: String): Boolean {
    if (checkResult) {
      project.getLogger().error("You haven't configured $fieldDescription. Please add $requiredValue to your build.gradle file.")
    }
    return checkResult
  }

  /**
   * Builds the map of download URLs
   */
  private fun buildMapOfGitHubDownloadLinks() : Map<String,String> {

    fun JsonArray<JsonObject>.releaseByLabel(label: String): JsonObject? =
      this.find { it["tag_name"] == label }

    fun JsonObject.downloadUrl() : String? {
      val assets = this["assets"] as? JsonArray<JsonObject>
      return assets?.mapNotNull {
          it["browser_download_url"]?.toString()
        }
        ?.find { it.endsWith(".jar") }
    }

    val specs = ReleasesSpec.load(project.configuredReleasesConfigFile)
    val client = GithubReleasesClient(
      user = project.configuredGithubUser,
      accessToken =  project.configuredGithubAccessToken,
      repository = project.configuredGithubRepository,
      apiUrl = project.configuredGithubApiUrl
    )
    val remoteReleases = client.getReleases()

    return specs?.relevantReleasesForDownloadUrls()
      ?.fold(initial=mutableMapOf()) fold@{links, release ->
        val key = "${release.numericJosmVersion}_Plugin-Url"
        val remoteRelease = remoteReleases?.releaseByLabel(release.label)
          ?: run {
            project.logger.warn(
              "Could not find a remote release for the release label " +
                "'${release.label}'. No download link included in the " +
                "MANIFEST file for JOSM release ${release.numericJosmVersion}"
            )
            return@fold links
          }
        val downloadUrl = remoteRelease?.downloadUrl() ?: run {
          project.logger.warn(
            "Could not find a jar download url for the remote release with " +
              "label '${release.label}'. No download link included in the " +
              "MANIFEST file for JOSM release ${release.numericJosmVersion}"
          )
          return@fold links
        }
        val value = "${release.label};$downloadUrl"

        links[key] = value
        links
      }
      ?: emptyMap()
  }

  /**
   * Returns a map containing all manifest attributes, which are set.
   * This map can then be fed into [org.gradle.api.java.archives.Manifest.attributes()]. That's already done automatically by the gradle-josm-plugin, so you normally don't need to call this yourself.
   *
   * Make sure the [langCompileTask] already ran before this method is called, otherwise not all translated descriptions are included in the Manifest.
   */
  public fun createJosmPluginJarManifest(): Map<String,String> {
    isRequiredFieldMissing(minJosmVersion == null, "the minimum JOSM version your plugin is compatible with", "josm.manifest.minJosmVersion = ‹a JOSM version›")
    isRequiredFieldMissing(project.version == Project.DEFAULT_VERSION, "the version of your plugin", "version = ‹a version number›")
    isRequiredFieldMissing(mainClass == null, "the main class of your plugin", "josm.manifest.mainClass = ‹full name of main class›")
    isRequiredFieldMissing(description == null, "the description of your plugin", "josm.manifest.description = ‹a textual description›")

    val missingException: GradleException = GradleException("The JOSM plugin ${project.name} misses required configuration options. See above for which options are missing.")

    val minJosmVersion: String = minJosmVersion ?: throw missingException
    val mainClass: String = mainClass ?: throw missingException
    val description: String = description ?: throw missingException
    val projectVersion: String = project.version.toString()
    if (projectVersion == Project.DEFAULT_VERSION) {
      throw missingException
    }

    // Required attributes
    val manifestAtts: MutableMap<String,String> = mutableMapOf<String, String>(
      "Created-By" to System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")",
      "Gradle-Version" to project.gradle.gradleVersion,
      "Groovy-Version" to GroovySystem.getVersion(),
      "Plugin-Class" to mainClass,
      "Plugin-Date" to String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", GregorianCalendar()),
      "Plugin-Description" to description,
      "Plugin-Mainversion" to minJosmVersion,
      "Plugin-Version" to projectVersion,
      "Plugin-Early" to loadEarly.toString(),
      "Plugin-Canloadatruntime" to canLoadAtRuntime.toString()
    )

    if (includeLinksToGithubReleases) {
      // Add download links to github releases
      manifestAtts.putAll(buildMapOfGitHubDownloadLinks())
    } else {
      // Add links to older versions of the plugin
      for (value in oldVersionDownloadLinks) {
        manifestAtts.put(value.minJosmVersion.toString() + "_Plugin-Url", value.pluginVersion + ';' + value.downloadURL.toString())
      }
    }

    // Add translated versions of the project description
    val langCompileTask = langCompileTask
    if (langCompileTask != null) {
      val langDir = File(langCompileTask.destinationDir, langCompileTask.subdirectory)
      if (langDir.exists() && langDir.canRead()) {
        val translations = LangReader().readLangFiles(langDir, project.extensions.josm.i18n.mainLanguage)
        val baseDescription = project.extensions.josm.manifest.description
        if (baseDescription != null) {
          translations.forEach {
            val translatedDescription = it.value[MsgId(MsgStr(baseDescription))]
            if (translatedDescription != null && translatedDescription.strings.isNotEmpty()) {
              manifestAtts["${it.key}_Plugin-Description"] = translatedDescription.strings.first()
            }
          }
        }
      }
    }
    for(entry in translatedDescriptions) {
      manifestAtts["${entry.key}_Plugin-Description"] = entry.value
    }

    // Optional attributes
    author?.let { manifestAtts.put("Author", it) }
    iconPath?.let { manifestAtts.put("Plugin-Icon", it) }
    loadPriority?.let { manifestAtts.put("Plugin-Stage", it.toString()) }
    website?.let { manifestAtts.put("Plugin-Link", it.toString()) }
    if (!pluginDependencies.isEmpty()) {
      manifestAtts.put("Plugin-Requires", pluginDependencies.joinToString(";"))
    }

    if (project.logger.isInfoEnabled()) {
      project.getLogger().info("The following lines will be added to the manifest of the plugin *.jar file:")
      for (e in manifestAtts.toSortedMap()) {
        project.getLogger().info("  ${e.key}: ${e.value}")
      }
    }
    return manifestAtts
  }

  /**
   * Adds a translation of the plugin description for a certain language.
   * @param language the language abbreviation (e.g. `de_AT` or `es`)
   * @param translatedDescription the description in the language given by the `language` parameter
   */
  public fun translatedDescription(language: String, translatedDescription: String) {
    if (!language.matches(Regex("[a-z]{2,3}(_[A-Z]{2})?"))) {
      throw IllegalArgumentException("The given language string '$language' is not a valid abbreviation for a language.")
    }
    if (translatedDescription.isEmpty()) {
      throw IllegalArgumentException("The translated description must not be empty")
    }
    translatedDescriptions.put(language, translatedDescription)
  }
}
