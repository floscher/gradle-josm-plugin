package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.io.PluginInfo
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.net.URL
import java.util.GregorianCalendar

open class GeneratePluginList : DefaultTask() {

  /**
   * Maps the plugin name to the manifest attributes and the download URL of the plugin
   */
  private val plugins: MutableList<PluginInfo> = mutableListOf()

  @Input
  val immutablePlugins = plugins.toList()

  /**
   * The file to which this task writes the plugin list, will be overwritten if it exists.
   * This parameter is required.
   */
  @OutputFile
  lateinit var outputFile: File

  /**
   * Optional parameter, converts a relative icon path (you decide relative to what,
   * this class does not make assumptions about that) to a Base64 representation.
   * This parameter is optional, by default or if it returns `null`, the icon path is added as-is to the list.
   */
  @Internal
  var iconBase64Provider: (String) -> String? = { _ -> null }

  /**
   * This field is only here, so Gradle can serialize [iconBase64Provider] for up-to-date-checking/caching
   */
  @Input
  val serializableIconProvider = iconBase64Provider as Serializable

  /**
   * A function that gives you a suffix that's appended to the plugin version. It takes the plugin name as an argument.
   */
  @Internal
  var versionSuffix: (String) -> String? = { _ -> '#' + String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", GregorianCalendar()) }

  /**
   * This field is only here, so Gradle can serialize [versionSuffix] for up-to-date-checking/caching
   */
  @Input
  val serializableVersionSuffix = versionSuffix as Serializable

  @TaskAction
  fun action() {
    logger.lifecycle("Writing list of ${plugins.size} plugins to ${outputFile.absolutePath} …")
    val fileBuilder = StringBuilder()

    plugins.sortedBy { it.pluginName }.forEach { (name, url, manifestAtts) ->
      fileBuilder
        .append(name)
        .append(';')
        .append(url)
        .append('\n')
      manifestAtts.forEach { key, value ->
        fileBuilder
          .append('\t')
          .append(key)
          .append(": ")
          .append(when (key) {
            JosmManifest.Attribute.PLUGIN_ICON -> iconBase64Provider.invoke(value) ?: value
            JosmManifest.Attribute.PLUGIN_VERSION -> value + versionSuffix.invoke(name)
            else -> value
          })
          .append('\n')
      }
    }

    if (!outputFile.parentFile.exists() && !outputFile.parentFile.mkdirs()) {
      throw TaskExecutionException(this, IOException("Can't create directory ${outputFile.parentFile.absolutePath}!"))
    }
    outputFile.writeText(fileBuilder.toString(), Charsets.UTF_8)
  }

  /**
   * Add a plugin that should appear in the list
   * @param name the name of the plugin *.jar file (including file extension), e.g. `MyAwesomePlugin.jar`
   * @param atts the main attributes of the plugin manifest, e.g. supplied by [JosmManifest.createJosmPluginJarManifest]
   * @param downloadUrl the URL from which the plugin can be downloaded
   */
  fun addPlugin(name: String, atts: Map<String, String>, downloadUrl: URL) {
    plugins.add(PluginInfo(name, downloadUrl, atts))
  }
}
