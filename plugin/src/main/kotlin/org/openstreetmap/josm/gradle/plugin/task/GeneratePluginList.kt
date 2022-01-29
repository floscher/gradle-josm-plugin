package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.io.PluginInfo
import org.openstreetmap.josm.gradle.plugin.util.toBase64DataUrl
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * A task that can write a plugin update site. JOSM can be pointed to a URL of such an update site,
 * so the plugins in that list can be installed via the regular JOSM update mechanism in the JOSM settings.
 *
 * @property iconPathToFile a function that returns a [File] object for a (relative) icon path.
 *  This is used to convert a manifest entry for [JosmManifest.Attribute.PLUGIN_ICON] from a relative path
 *  to a Base64-Data-URL. If you don't need this functionality, just pass `{ it -> null }`, which will always return `null`.
 */
public open class GeneratePluginList @Inject constructor(
  private val iconPathToFile: (String) -> File?
): DefaultTask() {

  /**
   * All plugins that should appear in the list (as [PluginInfo]s).
   */
  @get:Input
  public val plugins: SetProperty<PluginInfo> = project.objects.setProperty(PluginInfo::class.java)

  /**
   * The file to which this task writes the plugin list, will be overwritten if it exists.
   * This parameter is required.
   */
  @get:OutputFile
  public val outputFile: RegularFileProperty = project.objects.fileProperty()

  /**
   * Maps plugins to icon files, helper to connect [plugins] and [iconFiles].
   */
  private val iconFileMap: Provider<Map<PluginInfo, File>> = plugins.map { plugins ->
    plugins.mapNotNull { pi ->
      pi.manifestAtts.entries
        .firstOrNull { (key, _) -> key == JosmManifest.Attribute.PLUGIN_ICON.manifestKey }
        ?.value
        ?.let { iconPathToFile(it) }
        ?.let { pi to it }
    }.toMap()
  }

  /**
   * A list of icon files derived from [plugins] and [iconPathToFile].
   * This is marked as input files, so that changes in the icon files are detected and trigger a regeneration.
   */
  @get:InputFiles
  public val iconFiles: Provider<Set<File>> = iconFileMap.map { prop -> prop.values.map { it.canonicalFile }.toSet() }

  /**
   * A version suffix that's appended to the plugin version(s).
   */
  @get:Input
  public val versionSuffix: Property<String> = project.objects.property(String::class.java).convention("")

  @Internal
  final override fun getGroup(): String = "JOSM"
  final override fun setGroup(group: String?): Nothing = throw UnsupportedOperationException(
    "Can't change group of ${javaClass.name}!"
  )

  @TaskAction
  public open fun action() {
    val plugins: Set<PluginInfo> = plugins.also{ it.finalizeValue() }.get()
    val outputFile: File = outputFile.also { it.finalizeValue() }.asFile.get()

    logger.lifecycle("Writing list of ${plugins.size} plugin${if (plugins.size > 1) "s" else ""} to ${outputFile.absolutePath} …")

    if (!outputFile.parentFile.exists() && !outputFile.parentFile.mkdirs()) {
      throw TaskExecutionException(this, IOException("Can't create directory ${outputFile.parentFile.absolutePath}!"))
    }

    outputFile.writeText(
      plugins.sortedBy { it.pluginName }.flatMap {
        listOf("${it.pluginName}.jar;${it.downloadUri}") +
        it.manifestAtts.map { (key, value) ->
          "\t$key: " +
          when (key) {
            JosmManifest.Attribute.PLUGIN_ICON.manifestKey ->
              try {
                iconFileMap.get()[it]?.toBase64DataUrl()
              } catch (e: IOException) {
                logger.lifecycle("Error reading")
              } ?: value
            JosmManifest.Attribute.PLUGIN_VERSION.manifestKey ->
              value + versionSuffix.get()
            else -> value
          }
        }
      }.joinToString("\n", "", "\n")
    )

    logger.lifecycle("""
      |
      |The list contains:
      |${
        plugins.joinToString("\n", " * ") { pluginInfo ->
          pluginInfo.pluginName +
          (
            pluginInfo.manifestAtts.entries
              .firstOrNull { (key, _) -> key == JosmManifest.Attribute.PLUGIN_VERSION.manifestKey }
              ?.value
              ?.let { " ($it)" }
              ?: ""
          )
        }
      }
      |
      |${"By adding the following URL as a JOSM plugin update site (see tab 'Plugins' in expert mode), " +
        "you can load the current development state into a JOSM instance via the regular plugin update mechanism:"}
      |  ${outputFile.toURI()}
    """.trimMargin())
  }
}
