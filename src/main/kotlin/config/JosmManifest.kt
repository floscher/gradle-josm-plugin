package org.openstreetmap.josm.gradle.plugin.config

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import groovy.lang.GroovySystem
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.github.GithubReleasesClient
import org.openstreetmap.josm.gradle.plugin.github.ReleaseSpec
import org.openstreetmap.josm.gradle.plugin.github.onlyFallbackVersions
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangReader
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.task.LangCompile
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.lang.IllegalArgumentException
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
 * ```
 * <!-- edit the properties of this plugin in the file `gradle.properties` -->
 * <property file="${basedir}/gradle.properties"/>
 * ```
 *
 * The detailed documentation for each field below tells you, which one corresponds
 * to which Ant/Gradle property and to which attribute in the `MANIFEST.MF` file.
 *
 * For a successful build you'll have to at least set the following properties in this class:
 * [mainClass], [description], [minJosmVersion]
 * Your project also has to set a value for [Project.getVersion()]
 */
class JosmManifest(private val project: Project) {

  object Attribute {
    const val AUTHOR = "Author"
    const val CREATED_BY = "Created-By"
    const val CLASSPATH = "Class-Path"
    const val GRADLE_VERSION = "Gradle-Version"
    const val GROOVY_VERSION = "Groovy-Version"
    const val PLUGIN_CAN_LOAD_AT_RUNTIME = "Plugin-Canloadatruntime"
    const val PLUGIN_DATE = "Plugin-Date"
    const val PLUGIN_DEPENDENCIES = "Plugin-Requires"
    const val PLUGIN_DESCRIPTION = "Plugin-Description"
    const val PLUGIN_EARLY = "Plugin-Early"
    const val PLUGIN_ICON = "Plugin-Icon"
    const val PLUGIN_LOAD_PRIORITY = "Plugin-Stage"
    const val PLUGIN_MAIN_CLASS = "Plugin-Class"
    const val PLUGIN_MIN_JAVA_VERSION = "Plugin-Minimum-Java-Version"
    const val PLUGIN_MIN_JOSM_VERSION = "Plugin-Mainversion"
    const val PLUGIN_PLATFORM = "Plugin-Platform"
    const val PLUGIN_PROVIDES = "Plugin-Provides"
    const val PLUGIN_WEBSITE = "Plugin-Link"
    const val PLUGIN_VERSION = "Plugin-Version"

    fun pluginDescription(language: String) = "${language}_Plugin-Description"
    fun pluginDownloadLink(minJosmVersion: Int) = "${minJosmVersion}_Plugin-Url"
  }

  /**
   * The author of the plugin.
   *
   * **Default value:** the value of property `plugin.author` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.AUTHOR] (`Author`)
   */
  var author: String? = project.findProperty("plugin.author")?.toString()

  /**
   * Determines if the plugin needs a restart after installation. `true` if no restart is required, `false` otherwise.
   *
   * **Default value:** the value of property `plugin.canloadatruntime` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME] (`Plugin-Canloadatruntime`)
   */
  var canLoadAtRuntime: Boolean = project.findProperty("plugin.canloadatruntime")?.toString()?.toBoolean() ?: false

  /**
   * Additional paths that will be added to the classpath when JOSM is running.
   *
   * **Default value:** an empty set
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.CLASSPATH] (`Class-Path`)
   */
  val classpath: Set<String> = setOf()

  public fun classpath(path: String) {
    require(!path.contains(' '))
    (classpath as MutableSet).add(path)
  }

  /**
   * The description of what the plugin does.
   *
   * **Default value:** the value of property `plugin.description` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_DESCRIPTION] (`Plugin-Description`)
   */
  var description: String? = project.findProperty("plugin.description")?.toString()

  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via [Class.getResource](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResource(java.lang.String)).
   *
   * **Default value:** the value of property `plugin.icon` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_ICON] (`Plugin-Icon`)
   */
  var iconPath: String? = project.findProperty("plugin.icon")?.toString()

  /**
   * The task that processes the *.lang files for translations. This is set automatically and you normally don't have
   * to change it.
   */
  lateinit var langCompileTask: LangCompile

  /**
   * This can be set to `true`, when the plugin should load before the GUI classes of JOSM.
   *
   * **Default value:** The value of property `plugin.early` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_EARLY] (`Plugin-Early`)
   */
  var loadEarly: Boolean = project.findProperty("plugin.early")?.toString()?.toBoolean() ?: false

  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to `null`.
   *
   * **Default value:** The integer value of property `plugin.stage` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_LOAD_PRIORITY] (`Plugin-Stage`)
   */
  var loadPriority: Int? = project.findProperty("plugin.stage")?.toString()?.toIntOrNull()

  /**
   * The full name of the main class of the plugin.
   *
   * **Default value:** the value of property `plugin.class` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MAIN_CLASS] (`Plugin-Class`)
   */
  var mainClass: String? = project.findProperty("plugin.class")?.toString()

  /**
   * The minimum Java version needed to run this plugin.
   *
   * **Default value:** `null`
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MIN_JAVA_VERSION] (`Plugin-Minimum-Java-Version`)
   *
   * @since 0.6.1
   */
  var minJavaVersion: Int? = null
    set(value) {
      require(value == null || value > 0)
      field = value
    }

  /**
   * The minimum JOSM version with which the plugin is compatible.
   *
   * **Default value:** the value of property `plugin.main.version` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MIN_JOSM_VERSION] (`Plugin-Mainversion`)
   */
  var minJosmVersion: String? = project.findProperty("plugin.main.version")?.toString()

  /**
   * If `true`, load the old version download links from GitHub releases (see [GithubConfig]).
   * Values added through [oldVersionDownloadLink] are ignored, if this is set to `true`.
   *
   * **Default value:** `false`
   *
   * **Influenced MANIFEST.MF attributes:** [Attribute.pluginDownloadLink] (`([1-9][0-9]*)_Plugin-Url`)
   */
  var includeLinksToGithubReleases: Boolean = false

  /**
   * The platform for which this native implementation of a virtual plugin is made.
   * Must be set in conjunction with [provides].
   *
   * **Default value:** `null`
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Platform`
   *
   * @since 0.6.1
   */
  var platform: Platform? = null

  /**
   * Convenience method to set the platform using a [String] instead of a [Platform] enum value.
   * @throws IllegalArgumentException if the name given as parameter does not match exactly with one of the [Platform]s
   */
  @Throws(IllegalArgumentException::class)
  fun setPlatform(name: String) {
    platform = Platform.valueOf(name)
  }

  /**
   * The three platforms for which JOSM supports virtual plugins.
   */
  enum class Platform {
    OSX, WINDOWS, UNIXOID
  }

  /**
   * The name of a virtual plugin for which this plugin is a native implementation.
   * Must be set in conjunction with [platform].
   *
   * **Default value:** `null`
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Provides`
   *
   * @since 0.6.1
   */
  var provides: String? = null

  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * **Default value:** the value of property `plugin.requires` split at every semicolon (do not rely on the order, as it is not necessarily maintained) or `null` if that property is not set.
   * **Influenced MANIFEST.MF attribute:** `Plugin-Requires`
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
   * Contains the [description] translated to languages other than English.
   * Can be set via [translatedDescription].
   */
  private val translatedDescriptions: MutableMap<String, String> = mutableMapOf()

  /**
   * A URL pointing to a web resource describing the plugin.
   *
   * **Default value:** The value of property `plugin.link` as URL (might error out with a [java.net.MalformedURLException] on malformed URLs), or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Link`
   */
  var website: URL? = if (project.hasProperty("plugin.link")) URL(project.findProperty("plugin.link").toString()) else null

  /**
   * For compatibility with older JOSM versions, that are not supported by the current version of the plugin.
   * This field contains URLs where versions of the plugin can be downloaded, which are compatible with certain older JOSM versions.

   * See [oldVersionDownloadLink] on how to add entries to this attribute.
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
  fun oldVersionDownloadLink(minJosmVersion: Int, pluginVersion: String, downloadURL: URL) {
    oldVersionDownloadLinks.add(PluginDownloadLink(minJosmVersion, pluginVersion, downloadURL))
  }

  /**
   * Builds the map of download URLs
   */
  private fun buildMapOfGitHubDownloadLinks() : Map<String,String> {

    fun JsonObject.downloadUrl() : String? {
      val assets = this["assets"] as? JsonArray<*>
      return assets
        ?.mapNotNull {
          (it as? JsonObject)?.get("browser_download_url")?.toString()
        }
        ?.find { it.endsWith(".jar") }
    }

    val specs = ReleaseSpec.loadListFrom(project.extensions.josm.github.releasesConfig.inputStream())
    val client = GithubReleasesClient(project.extensions.josm.github, project.extensions.josm.github.apiUrl)
    val remoteReleases = client.getReleases()

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

        links[Attribute.pluginDownloadLink(release.minJosmVersion)] = value
        links
      }
  }

  private sealed class RequiredField() {
    class RequiredFieldValue(val keyValue: Pair<String, String>): RequiredField()
    class RequiredFieldUnset(val description: String, val fix: String): RequiredField()
  }

  /**
   * Creates a required field, which can be either [RequiredField.RequiredFieldValue] or [RequiredField.RequiredFieldUnset].
   * @param key the manifest key that should be used for this field
   * @param value the currently set value for this field. If this is `null`, always a [RequiredField.RequiredFieldUnset] will be returned.
   * @param description a textual description about the field in question (e.g. "the version of your plugin")
   * @param fix a hint on what should be added to `gradle.properties` in order to set the field in question (e.g. "josm.manifest.requiredValue = ‹some value›")
   * @return iff the [value] is not `null`, a [RequiredField.RequiredFieldValue] is returned. Otherwise a [RequiredField.RequiredFieldUnset] is returned.
   */
  private fun requiredField(key: String, value: String?, description: String, fix: String): RequiredField {
    return if (value != null) {
      RequiredField.RequiredFieldValue(key to value)
    } else {
      RequiredField.RequiredFieldUnset(description, fix)
    }
  }

  /**
   * Returns a map containing all manifest attributes, which are set.
   * This map can then be fed into [org.gradle.api.java.archives.Manifest.attributes]. That's already done automatically by the gradle-josm-plugin, so you normally don't need to call this yourself.
   *
   * Make sure the [langCompileTask] already ran before this method is called, otherwise not all translated descriptions are included in the Manifest.
   */
  fun createJosmPluginJarManifest(): Map<String,String> {
    val requiredFields = listOfNotNull(
      requiredField(Attribute.PLUGIN_MIN_JOSM_VERSION, minJosmVersion, "the minimum JOSM version your plugin is compatible with", "josm.manifest.minJosmVersion = ‹a JOSM version›"),
      requiredField(Attribute.PLUGIN_MAIN_CLASS, mainClass, "the main class of your plugin", "josm.manifest.mainClass = ‹full name of main class›"),
      requiredField(Attribute.PLUGIN_DESCRIPTION, description, "the description of your plugin", "josm.manifest.description = ‹a textual description›"),
      if (provides == null) null else requiredField(Attribute.PLUGIN_PLATFORM, platform?.toString(), "the platform for which this plugin is written", "josm.manifest.platform = ‹Osx, Windows or Unixoid›"),
      if (platform == null) null else requiredField(Attribute.PLUGIN_PROVIDES, provides, "the name of the virtual plugin for which this is an implementation", "josm.manifest.provides = ‹virtual plugin name›")
    )

    // Check that all required fields are set
    requiredFields.mapNotNull { if (it is RequiredField.RequiredFieldUnset) it else null }
      .takeIf { it.isNotEmpty() }
      ?.apply {
        throw GradleException(
          """
          |The JOSM plugin ${project.name} misses these required configuration options:
          | * ${this.joinToString("\n * ") { it.description } }
          |Add these lines to your `gradle.properties` files in order to fix this:
          |  ${this.joinToString("\n  ") { it.fix } }
          """.trimMargin()
        )
      }

    // Required attributes
    val manifestAtts: MutableMap<String,String?> = mutableMapOf(
      Attribute.CLASSPATH to classpath.joinToString(" "),
      Attribute.CREATED_BY to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
      Attribute.GRADLE_VERSION to project.gradle.gradleVersion,
      Attribute.GROOVY_VERSION to GroovySystem.getVersion(),
      Attribute.PLUGIN_DATE to String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", GregorianCalendar()),
      Attribute.PLUGIN_VERSION to project.version.toString(),
      Attribute.PLUGIN_EARLY to loadEarly.toString(),
      Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME to canLoadAtRuntime.toString(),

      // Optional attributes
      Attribute.AUTHOR to author,
      Attribute.PLUGIN_ICON to iconPath,
      Attribute.PLUGIN_LOAD_PRIORITY to loadPriority?.toString(),
      Attribute.PLUGIN_MIN_JAVA_VERSION to minJavaVersion?.toString(),
      Attribute.PLUGIN_PLATFORM to platform?.toString(),
      Attribute.PLUGIN_PROVIDES to provides,
      Attribute.PLUGIN_WEBSITE to website?.toString(),
      Attribute.PLUGIN_DEPENDENCIES to pluginDependencies.ifEmpty { null }?.joinToString(";")
    )
    // Add the required fields
    manifestAtts.putAll(requiredFields.mapNotNull { if (it is RequiredField.RequiredFieldValue) it.keyValue else null })

    if (includeLinksToGithubReleases) {
      // Add download links to github releases
      manifestAtts.putAll(buildMapOfGitHubDownloadLinks())
    }
    // Add manually specified links to older versions of the plugin
    for (value in oldVersionDownloadLinks) {
      manifestAtts[Attribute.pluginDownloadLink(value.minJosmVersion)] = "${value.pluginVersion};${value.downloadURL}"
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
              manifestAtts[Attribute.pluginDescription(it.key)] = translatedDescription.strings.first()
            }
          }
        }
      }
    }
    // Add the manually added translations of the plugin description
    for(entry in translatedDescriptions) {
      manifestAtts[Attribute.pluginDescription(entry.key)] = entry.value
    }

    if (project.logger.isInfoEnabled) {
      project.logger.info("The following lines will be added to the manifest of the plugin *.jar file:")
      for (e in manifestAtts.toSortedMap()) {
        project.logger.info("  ${e.key}: ${e.value}")
      }
    }
    return manifestAtts.mapNotNull { entry ->
      entry.value?.let { Pair(entry.key, it) } // only return entries with not-null value
    }.toMap()
  }

  /**
   * Add a translation of the plugin description for a certain language.
   * @param language the language abbreviation (e.g. `de_AT` or `es`)
   * @param translatedDescription the description in the language given by the `language` parameter
   */
  fun translatedDescription(language: String, translatedDescription: String) {
    require(language.matches(Regex("[a-z]{2,3}((_[A-Z]{2})|(-[a-z]+))?"))) {
      "The given language string '$language' is not a valid abbreviation for a language."
    }
    require(translatedDescription.isNotBlank()) {
      "The translated description must not be blank!"
    }
    translatedDescriptions[language] = translatedDescription
  }
}
