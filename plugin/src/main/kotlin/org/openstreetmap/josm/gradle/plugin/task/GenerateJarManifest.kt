package org.openstreetmap.josm.gradle.plugin.task;

import groovy.lang.GroovySystem
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.openstreetmap.josm.gradle.plugin.common.SplitStringToMaxByteLengthIterator
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.i18n.io.LangFileDecoder
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@CacheableTask
public open class GenerateJarManifest @Inject constructor(
  i18nCompileTask: TaskProvider<CompileToLang>
): DefaultTask() {
  public companion object {
    public const val MANIFEST_PATH: String = "META-INF/MANIFEST.MF"
  }

  @get: Input
  public val descriptionTranslations: Provider<Map<String, String>> = i18nCompileTask.map {
    val description = project.extensions.josm.manifest.description
    if (description == null) {
      mapOf()
    } else {
      val baseLangBytes: ByteArray? = it.outputDirectory.get().resolve("data/en.lang").takeIf { it.exists() }?.readBytes()
      if (baseLangBytes == null) {
        mapOf()
      } else {
        val otherLangFiles = it.outputDirectory.get().resolve("data")
          .listFiles { file: File -> file.extension == "lang" }
          ?.associate { it.nameWithoutExtension to it.readBytes() }
          ?: mapOf()

        LangFileDecoder.decodeMultipleLanguages("en", baseLangBytes, otherLangFiles)
          .filterKeys { it != "en" }
          .mapNotNull { (language, translations) ->
            translations.entries
              .firstOrNull { (key, _) ->
                key == MsgId(MsgStr(description))
              }
              ?.value
              ?.strings
              ?.singleOrNull()
              ?.let { JosmManifest.Attribute.pluginDescriptionKey(language) to it }
          }.toMap()
      }
    }
  }

  @get:Input
  public val predefinedAttributes: Provider<Map<String, String>> = project.provider {
    val josmManifest = project.extensions.josm.manifest
    val requiredFields = setOfNotNull(
      RequiredAttribute.create(
        JosmManifest.Attribute.PLUGIN_MIN_JOSM_VERSION,
        josmManifest.minJosmVersion,
        "the minimum JOSM version your plugin is compatible with"
      ),
      RequiredAttribute.create(
        JosmManifest.Attribute.PLUGIN_MAIN_CLASS,
        josmManifest.mainClass,
        "the full name of the main class of your plugin"
      ),
      RequiredAttribute.create(
        JosmManifest.Attribute.PLUGIN_DESCRIPTION,
        josmManifest.description,
        "the textual description of your plugin"
      ),
      if (josmManifest.provides == null) null else {
        RequiredAttribute.create(
          JosmManifest.Attribute.PLUGIN_PLATFORM,
          josmManifest.platform?.toString(),
          "the platform for which this plugin is written (must be either Osx, Windows or Unixoid)"
        )
      },
      if (josmManifest.platform == null) null else {
        RequiredAttribute.create(
          JosmManifest.Attribute.PLUGIN_PROVIDES,
          josmManifest.provides,
          "the name of the virtual plugin for which this is an implementation"
        )
      }
    )

    requiredFields.filterIsInstance<RequiredAttribute.Missing>()
      .takeIf { it.isNotEmpty() }
      ?.let { missingRequiredFields ->
        throw GradleException(
          """
          |The JOSM plugin misses these required configuration options:
          |${missingRequiredFields.joinToString("\n") { " * ${it.description}" }}
          |You can add these lines to your `gradle.properties` file in order to fix this:
          |${missingRequiredFields.joinToString("\n") { " * ${it.key.propertiesKey} = ‹${it.description}›" }}
          |Or alternatively add the equivalent lines to the `josm.manifest {}` block in your `build.gradle(.kts)` file.
          """.trimMargin()
        )
      }

    mapOf("Manifest-Version" to "1.0").plus(
    requiredFields.filterIsInstance<RequiredAttribute.Present>().map { it.key to it.value }
      .plus(
        setOf<Pair<JosmManifest.Attribute, String?>>(
          // Default attributes (always set)
          JosmManifest.Attribute.CREATED_BY to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
          JosmManifest.Attribute.GRADLE_VERSION to project.gradle.gradleVersion,
          JosmManifest.Attribute.GROOVY_VERSION to GroovySystem.getVersion(),
          JosmManifest.Attribute.PLUGIN_DATE to DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            ZonedDateTime.ofInstant(project.extensions.josm.manifest.pluginDate.get(), ZoneOffset.UTC)
          ),
          JosmManifest.Attribute.PLUGIN_VERSION to project.version.toString(),
          JosmManifest.Attribute.PLUGIN_EARLY to josmManifest.loadEarly.toString(),
          JosmManifest.Attribute.PLUGIN_CAN_LOAD_AT_RUNTIME to josmManifest.canLoadAtRuntime.toString(),

          // Optional attributes (can be null)
          JosmManifest.Attribute.AUTHOR to josmManifest.author,
          JosmManifest.Attribute.CLASSPATH to josmManifest.classpath.joinToString(" ").takeIf { it.isNotBlank() },
          JosmManifest.Attribute.PLUGIN_ICON to josmManifest.iconPath,
          JosmManifest.Attribute.PLUGIN_LOAD_PRIORITY to josmManifest.loadPriority?.toString(),
          JosmManifest.Attribute.PLUGIN_MIN_JAVA_VERSION to josmManifest.minJavaVersion?.toString(),
          JosmManifest.Attribute.PLUGIN_PLATFORM to josmManifest.platform?.toString(),
          JosmManifest.Attribute.PLUGIN_PROVIDES to josmManifest.provides,
          JosmManifest.Attribute.PLUGIN_WEBSITE to josmManifest.website?.toString(),
          JosmManifest.Attribute.PLUGIN_DEPENDENCIES to josmManifest.pluginDependencies.getOrElse(emptySet()).ifEmpty { null }?.joinToString(";")
        )
      )
      .mapNotNull { (key, value) -> value?.let { key.manifestKey to it } }
      .toMap()
      // Add download links to older GitHub releases
      .plus(if (josmManifest.includeLinksToGithubReleases) buildMapOfGitHubDownloadLinks(project) else mapOf())
      .toSortedMap()
      // Add manually specified links to older versions of the plugin
      .plus(
        josmManifest.oldVersionDownloadLinks
          .associate { JosmManifest.Attribute.pluginDownloadLinkKey(it.minJosmVersion) to "${it.pluginVersion};${it.downloadURL}" }
          .toSortedMap()
      )
      .plus(descriptionTranslations.get().toSortedMap())
    )
  }

  @get:Internal
  public val outputDirectory: Provider<Directory> = project.layout.buildDirectory.map { it.dir("josm-manifest") }

  @get:OutputFile
  public val outputFile: Provider<RegularFile> = outputDirectory.map { it.file(MANIFEST_PATH) }

  @TaskAction
  public fun action() {
    outputFile.get().asFile.writeText(
      predefinedAttributes.get().entries.joinToString("\n", postfix = "\n") {
        mainAttributeLine(it.key, it.value)
      }
    )
  }

  private fun mainAttributeLine(key: String, value: String): String {
    require(key.toByteArray().size <= 70) {
      "Main attributes in a MANIFEST.MF file must not be longer than 70 bytes"
    }
    require(!key.contains(Regex("[:\n\r\u0000]"))) {
      "Main attributes in a MANIFEST.MF file must not contain characters `CR`, `LF`, `NUL` or `:`!"
    }
    return SplitStringToMaxByteLengthIterator("$key: $value", 72, 71)
      .asSequence()
      .joinToString("\n ")
  }

  private sealed class RequiredAttribute {
    companion object {
      fun create(key: JosmManifest.Attribute, value: String?, description: String) =
        if (value == null) Missing(key, description) else Present(key, value)
    }
    data class Present(val key: JosmManifest.Attribute, val value: String): RequiredAttribute()
    data class Missing(val key: JosmManifest.Attribute, val description: String): RequiredAttribute()
  }
}
