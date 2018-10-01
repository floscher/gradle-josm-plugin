package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.GregorianCalendar

open class GeneratePluginList : DefaultTask() {

  /**
   * Maps the plugin name to the manifest attributes and the download URL of the plugin
   */
  @Internal
  private val plugins: MutableMap<String, Pair<Map<String, String>, URL>> = mutableMapOf()

  /**
   * The file to which this task writes the plugin list, will be overwritten if it exists.
   * This parameter is required.
   */
  @Internal
  lateinit var outputFile: File

  /**
   * Optional parameter, converts a relative icon path (you decide relative to what,
   * this class does not make assumptions about that) to a Base64 representation.
   * This parameter is optional, by default or if it returns `null`, the icon path is added as-is to the list.
   */
  @Internal
  var iconBase64Provider: (String) -> String? = { _ -> null }

  /**
   * A function that gives you a suffix that's appended to the plugin version. It takes the plugin name as an argument.
   */
  @Internal
  var versionSuffix: (String) -> String? = { _ -> '#' + String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", GregorianCalendar()) }

  init {
    project.afterEvaluate {
      outputs.file(outputFile)
    }
  }

  @TaskAction
  fun action() {
    val fileBuilder = StringBuilder()

    plugins.forEach { name, (manifest, url) ->
      fileBuilder
        .append(name)
        .append(';')
        .append(url)
        .append('\n')
      manifest.forEach { key, value ->
        fileBuilder
          .append('\t')
          .append(key)
          .append(": ")
          .append(when (key) {
            "Plugin-Icon" -> iconBase64Provider.invoke(value) ?: value
            "Plugin-Version" -> value + versionSuffix.invoke(name)
            else -> value
          })
          .append('\n')
      }
    }

    if (!outputFile.parentFile.exists()) {
      if (!outputFile.parentFile.mkdirs()) {
        throw TaskExecutionException(this, IOException("Can't create directory ${outputFile.parentFile.absolutePath}!"))
      }
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
    plugins.put(name, Pair(atts, downloadUrl))
  }
}
