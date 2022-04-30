package org.openstreetmap.josm.gradle.plugin.config

import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.IOException
import java.net.URL
import java.time.Instant

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

  enum class Attribute(val manifestKey: String, val propertiesKey: String? = null) {
    AUTHOR("Author", "plugin.author"),
    CREATED_BY("Created-By"),
    CLASSPATH("Class-Path"),
    GRADLE_VERSION("Gradle-Version"),
    GROOVY_VERSION("Groovy-Version"),
    PLUGIN_CAN_LOAD_AT_RUNTIME("Plugin-Canloadatruntime", "plugin.canloadatruntime"),
    PLUGIN_DATE("Plugin-Date"),
    PLUGIN_DEPENDENCIES("Plugin-Requires", "plugin.requires"),
    PLUGIN_DESCRIPTION("Plugin-Description", "plugin.description"),
    PLUGIN_EARLY("Plugin-Early", "plugin.early"),
    PLUGIN_ICON("Plugin-Icon", "plugin.icon"),
    PLUGIN_LOAD_PRIORITY("Plugin-Stage", "plugin.stage"),
    PLUGIN_MAIN_CLASS("Plugin-Class", "plugin.class"),
    PLUGIN_MIN_JAVA_VERSION("Plugin-Minimum-Java-Version"),
    PLUGIN_MIN_JOSM_VERSION("Plugin-Mainversion", "plugin.main.version"),
    PLUGIN_PLATFORM("Plugin-Platform", "josm.manifest.platform"),
    PLUGIN_PROVIDES("Plugin-Provides", "josm.manifest.provides"),
    PLUGIN_WEBSITE("Plugin-Link", "plugin.link"),
    PLUGIN_VERSION("Plugin-Version");

    companion object {
      @JvmStatic
      fun pluginDescriptionKey(language: String) = "${language}_Plugin-Description"
      @JvmStatic
      fun pluginDownloadLinkKey(minJosmVersion: Int) = "${minJosmVersion}_Plugin-Url"
    }
  }

  /**
   * The author of the plugin.
   *
   * **Default value:** the value of property `plugin.author` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.AUTHOR] (`Author`)
   */
  var author: String? = Attribute.AUTHOR.propertiesKey?.let { project.findProperty(it) }?.toString()

  /**
   * Determines if the plugin needs a restart after installation. `true` if no restart is required, `false` otherwise.
   *
   * **Default value:** the value of property `plugin.canloadatruntime` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME] (`Plugin-Canloadatruntime`)
   */
  var canLoadAtRuntime: Boolean = Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME.propertiesKey?.let { project.findProperty(it) }?.toString()?.toBoolean() ?: false

  /**
   * Additional paths that will be added to the classpath when JOSM is running.
   *
   * **Default value:** an empty set
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.CLASSPATH] (`Class-Path`)
   */
  val classpath: MutableList<String> = mutableListOf()

  public fun classpath(path: String) {
    require(!path.contains(' ')) {
      "A classpath must not contain space characters! If you want to add more than one, add them separately."
    }
    classpath.add(path)
  }

  /**
   * The description of what the plugin does.
   *
   * **Default value:** the value of property `plugin.description` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_DESCRIPTION] (`Plugin-Description`)
   */
  var description: String? = Attribute.PLUGIN_DESCRIPTION.propertiesKey?.let { project.findProperty(it) }?.toString()

  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via [Class.getResource](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResource(java.lang.String)).
   *
   * **Default value:** the value of property `plugin.icon` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_ICON] (`Plugin-Icon`)
   */
  var iconPath: String? = Attribute.PLUGIN_ICON.propertiesKey?.let { project.findProperty(it) }?.toString()

  /**
   * This can be set to `true`, when the plugin should load before the GUI classes of JOSM.
   *
   * **Default value:** The value of property `plugin.early` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_EARLY] (`Plugin-Early`)
   */
  var loadEarly: Boolean = Attribute.PLUGIN_EARLY.propertiesKey?.let { project.findProperty(it) }?.toString()?.toBoolean() ?: false

  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to `null`.
   *
   * **Default value:** The integer value of property `plugin.stage` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_LOAD_PRIORITY] (`Plugin-Stage`)
   */
  var loadPriority: Int? = Attribute.PLUGIN_LOAD_PRIORITY.propertiesKey?.let { project.findProperty(it) }?.toString()?.toIntOrNull()

  /**
   * The full name of the main class of the plugin.
   *
   * **Default value:** the value of property `plugin.class` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MAIN_CLASS] (`Plugin-Class`)
   */
  var mainClass: String? = Attribute.PLUGIN_MAIN_CLASS.propertiesKey?.let { project.findProperty(it) }?.toString()

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
  var minJosmVersion: String? = Attribute.PLUGIN_MIN_JOSM_VERSION.propertiesKey?.let { project.findProperty(it) }?.toString()

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
  var platform: Platform? = Attribute.PLUGIN_PLATFORM.propertiesKey
    ?.let { project.findProperty(it) }
    ?.toString()
    ?.let { Platform.valueOf(it) }

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
   * Provides an [Instant] that is supposed to reflect the point in time when the plugin was created.
   * If available, we use the timestamp from version control, because that makes the build more reproducible,
   * which amongst other things enables better build caching in Gradle. Also it's probably more meaningful to
   * anyone reading the manifest, than the time the *.jar file was built from the sources.
   *
   * **Default value:** If you build in a git-repository, then the commit timestamp of the current `HEAD` commit is used.
   * Otherwise the current time is used.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Date` ([Attribute.PLUGIN_DATE])
   *
   * @since 0.8.1
   */
  public val pluginDate: Property<Instant> = project.objects.property(Instant::class.java)
    .convention(
      project.provider {
        try {
          Git.open(project.projectDir).repository.let { repo ->
            Instant.ofEpochSecond(repo.parseCommit(repo.resolve("HEAD")).commitTime.toLong())
          }
        } catch (e: IOException) {
          project.logger.warn("Failed to determine git committer timestamp. Falling back to the current time.")
          Instant.now()
        }
      }
    )

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
  var provides: String? = Attribute.PLUGIN_PROVIDES.propertiesKey
    ?.let { project.findProperty(it) }
    ?.toString()

  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * **Default value:** the value of property `plugin.requires` split at every semicolon (do not rely on the order, as it is not necessarily maintained) or `null` if that property is not set.
   * **Influenced MANIFEST.MF attribute:** `Plugin-Requires`
   */
  val pluginDependencies: SetProperty<String> = project.objects.setProperty(String::class.java).convention(
    Attribute.PLUGIN_DEPENDENCIES.propertiesKey
      ?.let { project.findProperty(it) }
      ?.toString()
      ?.let { defaultValue ->
        defaultValue.split(";")
          .filter { it.isNotBlank() }
          .map { it.trim() }
      }
  )

  /**
   * A URL pointing to a web resource describing the plugin.
   *
   * **Default value:** The value of property `plugin.link` as URL (might error out with a [java.net.MalformedURLException] on malformed URLs), or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Link`
   */
  var website: URL? = Attribute.PLUGIN_WEBSITE.propertiesKey?.let { project.findProperty(it) }?.let { URL(it.toString()) }

  /**
   * For compatibility with older JOSM versions, that are not supported by the current version of the plugin.
   * This field contains URLs where versions of the plugin can be downloaded, which are compatible with certain older JOSM versions.
   *
   * See [oldVersionDownloadLink] on how to add entries to this attribute.
   * The URL value points to a location where the plugin can be downloaded from and the integer key denotes the minimum JOSM version that the plugin at that location is compatible with.
   */
  internal val oldVersionDownloadLinks: MutableSet<PluginDownloadLink> = mutableSetOf()

  internal inner class PluginDownloadLink(val minJosmVersion: Int, val pluginVersion: String, val downloadURL: URL)

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
}
