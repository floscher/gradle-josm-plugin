package org.openstreetmap.josm.gradle.plugin.config

import org.eclipse.jgit.api.Git
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.util.Locale

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
public class JosmManifest(private val project: Project) {

  public enum class Attribute(public val manifestKey: String, public val propertiesKey: String? = null) {
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

    public companion object {
      @JvmStatic
      public fun pluginDescriptionKey(language: String): String = "${language}_Plugin-Description"
      @JvmStatic
      public fun pluginDownloadLinkKey(minJosmVersion: Int): String = "${minJosmVersion}_Plugin-Url"
    }
  }

  private fun Attribute.findProperty(): String? = propertiesKey?.let { project.findProperty(it) }?.toString()

  /**
   * The author of the plugin.
   *
   * **Default value:** the value of property `plugin.author` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.AUTHOR] (`Author`)
   */
  public var author: String?
    get() = _author.get()
    set(value) { _author.set(value) }

  internal val _author: Property<String?> = project.objects.property(String::class.java)
    .convention(Attribute.AUTHOR.findProperty())

  /**
   * Determines if the plugin needs a restart after installation. `true` if no restart is required, `false` otherwise.
   *
   * **Default value:** the value of property `plugin.canloadatruntime` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME] (`Plugin-Canloadatruntime`)
   */
  public var canLoadAtRuntime: Boolean
    get() = _canLoadAtRuntime.get()
    set(value) = _canLoadAtRuntime.set(value)

  internal val _canLoadAtRuntime: Property<Boolean> = project.objects.property(Boolean::class.java)
    .convention(Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME.findProperty()?.toBoolean() ?: false)

  /**
   * Additional paths that will be added to the classpath when JOSM is running.
   *
   * Use `classpath()` in order to add paths to this list.
   *
   * **Default value:** an empty set
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.CLASSPATH] (`Class-Path`)
   */
  public val classpath: List<String>
    get() = _classpath.get()

  internal val _classpath = project.objects.listProperty(String::class.java).convention(listOf())

  /**
   * Add one or more paths to [classpath].
   * None of the paths can contain any whitespace!
   * @param path a path to add
   * @param morePaths more paths to add (if any)
   */
  public fun classpath(path: String, vararg morePaths: String) {
    val allPaths = (listOf(path) + morePaths)
    require(allPaths.none { it.isBlank() || it.contains(Regex("\\s")) }) {
      "A classpath must not contain whitespace characters! Make sure to escape paths correctly (e.g. space → %20). " +
      "If you want to add more than one path, add them separately."
    }
    _classpath.addAll(allPaths)
  }

  /**
   * The description of what the plugin does.
   *
   * **Default value:** the value of property `plugin.description` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_DESCRIPTION] (`Plugin-Description`)
   */
  public var description: String
    get() = _description.get()
    set(value) = _description.set(value)

  internal val _description: Property<String> = project.objects.property(String::class.java)
    .convention(Attribute.PLUGIN_DESCRIPTION.findProperty())

  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via [Class.getResource](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResource(java.lang.String)).
   *
   * **Default value:** the value of property `plugin.icon` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_ICON] (`Plugin-Icon`)
   */
  public var iconPath: String?
    get() = _iconPath.get()
    set(value) = _iconPath.set(value)

  internal val _iconPath: Property<String?> = project.objects.property(String::class.java)
    .convention(Attribute.PLUGIN_ICON.findProperty())

  /**
   * This can be set to `true`, when the plugin should load before the GUI classes of JOSM.
   *
   * **Default value:** The value of property `plugin.early` or `false` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_EARLY] (`Plugin-Early`)
   */
  public var loadEarly: Boolean
    get() = _loadEarly.get()
    set(value) = _loadEarly.set(value)

  internal val _loadEarly: Property<Boolean> = project.objects.property(Boolean::class.java)
    .convention(Attribute.PLUGIN_EARLY.findProperty()?.toBoolean() ?: false)

  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to `null`.
   *
   * **Default value:** The integer value of property `plugin.stage` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_LOAD_PRIORITY] (`Plugin-Stage`)
   */
  public var loadPriority: Int?
    get() = _loadPriority.get()
    set(value) = _loadPriority.set(value)

  internal val _loadPriority: Property<Int?> = project.objects.property(Int::class.java)
    .convention(Attribute.PLUGIN_LOAD_PRIORITY.findProperty()?.toIntOrNull())

  /**
   * The full name of the main class of the plugin.
   *
   * **Default value:** the value of property `plugin.class` or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MAIN_CLASS] (`Plugin-Class`)
   */
  public var mainClass: String
    get() = _mainClass.get()
    set(value) = _mainClass.set(value)

  internal val _mainClass: Property<String> = project.objects.property(String::class.java)
    .convention(Attribute.PLUGIN_MAIN_CLASS.findProperty())

  /**
   * The minimum Java version needed to run this plugin.
   *
   * **Default value:** `null`
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MIN_JAVA_VERSION] (`Plugin-Minimum-Java-Version`)
   *
   * @since 0.6.1
   */
  public var minJavaVersion: Int?
    get() = _minJavaVersion.orNull?.run { ordinal + 1 }
    set(value) {
      value.toJavaVersion()?.let {
        _minJavaVersion.set(it)
      }
    }

  internal val _minJavaVersion: Property<JavaVersion?> = project.objects.property(JavaVersion::class.java)
    .convention(Attribute.PLUGIN_MIN_JAVA_VERSION.findProperty()?.toIntOrNull()?.toJavaVersion())


  /**
   * @since 0.8.1
   */
  public fun setMinJavaVersion(version: JavaVersion) {
    _minJavaVersion.set(version)
  }

  private fun Int?.toJavaVersion(): JavaVersion? = when {
    this == null -> null
    this > 0 -> JavaVersion.forClassVersion(this + 44)
    else -> throw IllegalArgumentException("Java Version must be positive (given is: $this)!")
  }

  /**
   * The minimum JOSM version with which the plugin is compatible.
   * It is required to set this to a non-null value, otherwise the plugin can't be built.
   *
   * **Default value:** the value of property `plugin.main.version`.
   *
   * **Influenced MANIFEST.MF attribute:** [Attribute.PLUGIN_MIN_JOSM_VERSION] (`Plugin-Mainversion`)
   */
  public var minJosmVersion: String
    get() = _minJosmVersion.get()
    set(value) = _minJosmVersion.set(value)

  internal val _minJosmVersion: Property<String> = project.objects.property(String::class.java)
    .convention(Attribute.PLUGIN_MIN_JOSM_VERSION.findProperty())

  /**
   * If `true`, load the old version download links from GitHub releases (see [GithubConfig]).
   * Values added through [oldVersionDownloadLink] are ignored, if this is set to `true`.
   *
   * **Default value:** `false`
   *
   * **Influenced MANIFEST.MF attributes:** [Attribute.pluginDownloadLink] (`([1-9][0-9]*)_Plugin-Url`)
   */
  public var includeLinksToGithubReleases: Boolean
    get() = _includeLinksToGithubReleases.get()
    set(value) = _includeLinksToGithubReleases.set(value)

  internal val _includeLinksToGithubReleases: Property<Boolean> = project.objects.property(Boolean::class.java)
    .convention(false)

  /**
   * The platform for which this native implementation of a virtual plugin is made.
   * Must be set in conjunction with [provides].
   *
   * Allowed values are: Osx, Windows, Unixoid
   *
   * **Default value:** `null`
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Platform`
   *
   * @since 0.6.1
   */
  public var platform: Platform?
    get() = _platform.get()
    set(value) = _platform.set(value)

  internal val _platform: Property<Platform?> = project.objects.property(Platform::class.java)
    .convention(Attribute.PLUGIN_PLATFORM.findProperty()?.let { Platform.valueOf(it) })

  /**
   * Convenience method to set the platform using a [String] instead of a [Platform] enum value.
   * @throws IllegalArgumentException if the name given as parameter does not match exactly with one of the [Platform]s
   */
  @Throws(IllegalArgumentException::class)
  public fun setPlatform(name: String) {
    platform = Platform.valueOf(name.uppercase(Locale.ROOT))
  }

  /**
   * The three platforms for which JOSM supports virtual plugins.
   */
  public enum class Platform {
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
  public var provides: String?
    get() = _provides.get()
    set(value) = _provides.set(value)

  internal val _provides: Property<String?> = project.objects.property(String::class.java)
    .convention(Attribute.PLUGIN_PROVIDES.findProperty())

  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * **Default value:** the value of property `plugin.requires` split at every semicolon (do not rely on the order, as it is not necessarily maintained) or `null` if that property is not set.
   * **Influenced MANIFEST.MF attribute:** `Plugin-Requires`
   */
  public val pluginDependencies: SetProperty<String> = project.objects.setProperty(String::class.java).convention(
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
   * If you are using git: The version number is set automatically to reflect the output of `git describe --always`.
   * So just tag your release commits using a command like `git tag -a v1.2.3` and the version of your release builds
   * will automatically be set to `1.2.3`. Note that the use of the `-a` flag (or alternatively the `-s` flag) is
   * crucial, non-annotated tags are ignored for determining the version number.
   *
   * The project version [Project.getVersion] is used as version in the manifest.
   * So modify that directly by calling [Project.setVersion] if you really want to modify the version value.
   *
   * @since 0.8.1
   */
  public val version: Provider<String> = project.provider {
    project.version.toString()
  }

  /**
   * A URL pointing to a web resource describing the plugin.
   *
   * **Default value:** The value of property `plugin.link` as URL (might error out with a [java.net.MalformedURLException] on malformed URLs), or `null` if that property is not set.
   *
   * **Influenced MANIFEST.MF attribute:** `Plugin-Link`
   */
  public var website: URL?
    get() = _website.get()
    set(value) = _website.set(value)

  internal val _website: Property<URL?> = project.objects.property(URL::class.java)
    .convention(Attribute.PLUGIN_WEBSITE.findProperty()?.let { URL(it) })

  /**
   * @since 0.8.1
   */
  @Throws(MalformedURLException::class)
  public fun website(url: String) {
    _website.set(URL(url))
  }

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
  public fun oldVersionDownloadLink(minJosmVersion: Int, pluginVersion: String, downloadURL: URL) {
    oldVersionDownloadLinks.add(PluginDownloadLink(minJosmVersion, pluginVersion, downloadURL))
  }
}
