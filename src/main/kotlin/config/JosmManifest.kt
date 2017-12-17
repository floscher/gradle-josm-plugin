package org.openstreetmap.josm.gradle.plugin.config

import groovy.lang.GroovySystem
import java.net.URL
import java.util.GregorianCalendar
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * The info that will be written into the manifest file of the plugin *.jar
 *
 * <p>Most of the attributes are corresponding to the ones listed in
 * <a href="https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins#ThemanifestfileforaJOSMplugin">the documentation for developing JOSM plugins</a>.
 * But note that some of them have been renamed a bit to better express what each attribute is good for.</p>
 *
 * <p>The properties used by the Ant build (e.g. {@code plugin.author} or {@code plugin.description}) are automatically picked up by both Ant and Gradle, if you define them in your {@gradle.properties} file and load them into your {@code build.xml} file using the following snippet:
 * <pre>
 * &lt;!-- edit the properties of this plugin in the file `gradle.properties` -->
 * &lt;property file="${basedir}/gradle.properties"/>
 * </pre></p>
 *
 * <p>The detailed documentation for each field below tells you, which one corresponds to which Ant/Gradle property and to which attribute in the MANIFEST.MF file.</p>
 */
class JosmManifest(project: Project) {
  private val project: Project = project

  /**
   * The author of the plugin.
   *
   * <dl>
   *   <dt><strong>Default value:</strong></dt><dd>the value of property <code>plugin.author</code> or <code>null</code> if that property is not set.</dd>
   *   <dt><strong>Influenced MANIFEST.MF attribute:</strong></dt><dd>{@code Author}</dd>
   * </dl>
   */
  var author: String? = project.findProperty("plugin.author")?.toString()

  /**
   * Determines if the plugin needs a restart after installation. <code>true</code> if no restart is required, <code>false</code> otherwise.
   *
   * <p>
   *   <dt><strong>Default value:</strong></dt><dd>the value of property <code>plugin.canloadatruntime</code> or <code>false</code> if that property is not set.</dd>
   *   <dt><strong>Influenced MANIFEST.MF attribute:</strong></dt><dd>{@code Plugin-Canloadatruntime}</dd>
   * </p>
   */
  var canLoadAtRuntime: Boolean = project.findProperty("plugin.canloadatruntime")?.toString()?.toBoolean() ?: false

  /**
   * The description of what the plugin does.
   *
   * <dl>
   *   <dt><strong>Default value:</strong></dt><dd>the value of property <code>plugin.description</code> or <code>null</code> if that property is not set.</dd>
   *   <dt><strong>Influenced MANIFEST.MF attribute:</strong></dt><dd>{@code Plugin-Description}</dd>
   * </dl>
   */
  var description: String? = project.findProperty("plugin.description")?.toString()

  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via <code>getClass.getResource()</code>.
   *
   * <dl>
   *   <dt>Default value:</dt><dd>the value of property <code>plugin.icon</code> or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Icon}</dd>
   * </dl>
   */
  var iconPath: String? = project.findProperty("plugin.icon")?.toString()

  /**
   * This can be set to <code>true</code>, when the plugin should load before the GUI classes of JOSM.
   *
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>The value of property <code>plugin.early</code> or <code>false</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Early}</dd>
   * </dl>
   */
  var loadEarly: Boolean = project.findProperty("plugin.early")?.toString()?.toBoolean() ?: false

  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to <code>null</code>.
   *
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>The integer value of property <code>plugin.stage</code> or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Stage}</dd>
   * </dl>
   */
  var loadPriority: Int? = project.findProperty("plugin.stage")?.toString()?.toIntOrNull()

  /**
   * The full name of the main class of the plugin
   *
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>the value of property <code>plugin.class</code> or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Class}</dd>
   * </dl>
   */
  var mainClass: String? = project.findProperty("plugin.class")?.toString()

  /**
   * The minimum JOSM version with which the plugin is compatible.<br>
   * This field is required!
   *
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>the value of property <code>plugin.main.version</code> or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Mainversion}</dd>
   * </dl>
   */
  var minJosmVersion: String? = project.findProperty("plugin.main.version")?.toString()

  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>the value of property <code>plugin.requires</code> split at every semicolon (do not rely on the order, as it is not necessarily maintained) or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>Plugin-Requires</dd>
   * </dl>
   */
  val pluginDependencies: MutableSet<String> = mutableSetOf()

  init {
    // Fill the map containing the plugin dependencies
    val requirements: String? = project.findProperty("plugin.requires")?.toString()
    if (requirements != null) {
      val dependencyArray: List<String> = requirements.split(';')
      for (dependency in dependencyArray) {
        if (dependency.trim().length >= 1) {
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
   * <dl>
   *   <dt><strong>Default:</strong></dt><dd>The value of property <code>plugin.link</code> as URL (might error out with a {@link java.net.MalformedURLException} on malformed URLs), or <code>null</code> if that property is not set.</dd>
   *   <dt>Influenced MANIFEST.MF attribute:</dt><dd>{@code Plugin-Link}</dd>
   * </dl>
   */
  var website: URL? = if (project.hasProperty("plugin.link")) URL(project.findProperty("plugin.link").toString()) else null

  /**
   * For compatibility with older JOSM versions, that are not supported by the current version of the plugin.
   * This field contains URLs where versions of the plugin can be downloaded, which are compatible with certain older JOSM versions.
   * @see {@link #oldVersionDownloadLink(int, String, URL)} on how to add entries to this attribute.
   * The URL value points to a location where the plugin can be downloaded from and the integer key denotes the minimum JOSM version that the plugin at that location is compatible with.
   */
  private val oldVersionDownloadLinks: MutableSet<PluginDownloadLink> = mutableSetOf()

  private inner class PluginDownloadLink(minJosmVersion: Int, pluginVersion: String, downloadURL: URL) {
    val downloadURL: URL = downloadURL
    val minJosmVersion: Int = minJosmVersion
    val pluginVersion: String = pluginVersion
  }

  /**
   * Add a link to an earlier release of the plugin, that is compatible with JOSM versions, with which the current version is no longer compatible.
   * <dl>
   *   <dt><strong>Influenced MANIFEST.MF attribute:</strong></dt><dd>{@code ([1-9][0-9]*)_Plugin-Url} (Any attribute that matches this <a href="https://en.wikipedia.org/wiki/Regular_expression">RegEx</a>. The number in the beginning is determined by the parameter {@code minJosmVersion}.)</dd>
   * </dl>
   *
   * @param minJosmVersion the minimum JOSM version with which the linked plugin is compatible
   * @param pluginVersion the version number of the linked plugin
   * @param downloadURL the URL where the linked plugin can be downloaded from
   */
  public fun oldVersionDownloadLink(minJosmVersion: Int, pluginVersion: String, downloadURL: URL) {
    oldVersionDownloadLinks.add(PluginDownloadLink(minJosmVersion, pluginVersion, downloadURL))
  }

  /**
   * Logs an error message when the parameter {@code checkResult} is true.
   * @param checkResult a boolean value that is true when the field in question is missing
   * @param fieldDescription a textual description of the field (e.g. "the version of your plugin")
   * @param requiredValue the property which needs to be set in order to correct for this error (e.g. "josm.manifest.requiredValue = ‹some value›")
   * @return always returns the parameter {@code checkResult}
   */
  private fun isRequiredFieldMissing(checkResult: Boolean, fieldDescription: String, requiredValue: String): Boolean {
    if (checkResult) {
      project.getLogger().error("You haven't configured $fieldDescription. Please add $requiredValue to your build.gradle file.")
    }
    return checkResult
  }

  /**
   * Returns a map containing all manifest attributes, which are set.
   * This map can then be fed into {@link org.gradle.api.java.archives.Manifest#attributes(java.util.Map)}. That's already done automatically by the gradle-josm-plugin, so you normally don't need to call this yourself.
   */
  public fun createJosmPluginJarManifest(project: Project): Map<String,String> {
    isRequiredFieldMissing(minJosmVersion == null, "the minimum JOSM version your plugin is compatible with", "josm.manifest.minJosmVersion = ‹a JOSM version›")
    isRequiredFieldMissing(project.version == Project.DEFAULT_VERSION, "the version of your plugin", "version = ‹a version number›")
    isRequiredFieldMissing(mainClass == null, "the main class of your plugin", "josm.manifest.mainClass = ‹full name of main class›")
    isRequiredFieldMissing(description == null, "the description of your plugin", "josm.manifest.description = ‹a textual description›")

    val missingException: GradleException = GradleException("The JOSM plugin ${project.name} misses required configuration options. See above for which options are missing.")

    val minJosmVersion: String = minJosmVersion ?: throw missingException
    val projectVersion: String = project.version?.toString() ?: throw missingException
    val mainClass: String = mainClass ?: throw missingException
    val description: String = description ?: throw missingException

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

    // Add links to older versions of the plugin
    for(value in oldVersionDownloadLinks) {
      manifestAtts.put(value.minJosmVersion.toString() + "_Plugin-Url",  value.pluginVersion + ';' + value.downloadURL.toString())
    }

    // Add translated versions of the project description
    for(entry in translatedDescriptions) {
      manifestAtts.put(entry.key+"_Plugin-Description", entry.value)
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
   * @param language the language abbreviation (e.g. {@code de_AT} or {@code es})
   * @param translatedDescription the description in the language given by the {@code language} parameter
   */
  public fun translatedDescription(language: String, translatedDescription: String) {
    if (!language.matches(Regex("[a-z]{2,3}(_[A-Z]{2})?"))) {
      throw IllegalArgumentException("The given language string '$language' is not a valid abbreviation for a language.")
    }
    if (translatedDescription.length <= 0) {
      throw IllegalArgumentException("The translated description must not be empty")
    }
    translatedDescriptions.put(language, translatedDescription)
  }
}
