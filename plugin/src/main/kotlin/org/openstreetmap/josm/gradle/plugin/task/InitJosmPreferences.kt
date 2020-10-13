package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.util.VERSION_SNAPSHOT
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File

/**
 * Copies the plugin and all required plugins to JOSM_HOME.
 * Creates a `preferences-init.xml` file that will initialize the settings of the JOSM instance that is started by [RunJosmTask]s.
 * Custom settings can be provided by setting [JosmPluginExtension.initialPreferences].
 */
open class InitJosmPreferences: DefaultTask() {
  @get:InputFiles
  val pluginDistTask: TaskProvider<out Sync> = project.tasks.named("dist", Sync::class.java)

  @get:Input
  val pluginName: Provider<String> = project.provider { project.extensions.josm.pluginName }

  @get:InputFiles
  val requiredPluginConfig: Provider<Configuration> = project.configurations.named("requiredPlugin")

  @get:Input
  val requiredPluginsTemplate = InitJosmPreferences::class.java.getResource("/requiredPluginConfigTemplate.xml").readText()

  @get:Input
  val initialJosmPrefs = project.extensions.josm.initialPreferences

  @OutputFile
  val preferencesInitFile: Provider<RegularFile> = project.layout.buildDirectory.file(".josm/preferences-init.xml")

  @OutputDirectory
  val pluginDir: DirectoryProperty = project.objects.directoryProperty().fileProvider(
    project.provider {
      File(project.extensions.josm.tmpJosmUserdataDir, "plugins")
    }
  )

  @Internal
  override fun getDescription(): String = "Sync the required plugin *.jar files (including the one produced by the task ${pluginDistTask.get().path}) to ${pluginDir.get().asFile.absolutePath} .\n" +
    "Also create the file with the initial preferences for the tasks that run a JOSM instance: ${preferencesInitFile.get().asFile.absolutePath}"
  override fun setDescription(description: String?) = throw UnsupportedOperationException("Description can't be modified for ${InitJosmPreferences::class.simpleName}!")

  @TaskAction
  fun action() {
    // TODO: The following lines are for backwards compatibility with the `josmConfigDir` setting in the `JosmPluginExtension`. Remove when no longer needed
    val deprecatedPreferencesFile = File(project.extensions.josm.josmConfigDir, "preferences.xml")
    val prefFragment = if (deprecatedPreferencesFile.exists()) {
      "^.*<preferences[^>]+>(.+)</preferences>.*$".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        .matchEntire(deprecatedPreferencesFile.readText())
        ?.groupValues
        ?.get(1)
    } else {
      null
    }
    if (prefFragment != null) {
      val logDeprecationWarning = {
        logger.error("""
            |Deprecation warning:
            |====================
            |Please set default JOSM preferences by setting the following in your `build.gradle(.kts)`
            |josm {
            |  initialPreferences.set(
            |    "${prefFragment.trimIndent().lines().joinToString("\\n\" +\n    \"") { it.replace("\"", "\\\"") }}"
            |  )
            |}
            |instead of using ${deprecatedPreferencesFile.absolutePath} !
            |This change will become mandatory soon with one of the next releases of the gradle-josm-plugin.
            |""".trimMargin()
        )
      }
      logDeprecationWarning()
      project.gradle.buildFinished { logDeprecationWarning() }
    }
    // TODO: Remove until here, when no longer needed.

    project.sync {
      it.from(pluginDistTask)
      it.from(requiredPluginConfig)
      it.rename("(.*)-(${VERSION_SNAPSHOT})?\\.jar", "$1.jar")
      it.into(pluginDir)
    }

    logger.lifecycle("Write required plugin config to ${preferencesInitFile.get().asFile.absolutePath} …")
    preferencesInitFile.get().asFile.writeText(
      requiredPluginsTemplate
        .replace("{{{INITIAL_PREFERENCES}}}", ((prefFragment ?: "") + initialJosmPrefs.get()).replaceIndent(" ".repeat(4)))
        .replace(
          "{{{PLUGIN_LIST_ENTRIES}}}",
          requiredPluginConfig.get()
            .dependencies
            .map { it.name }
            .plus(pluginName.get())
            .joinToString("\n      ") { "<entry value=\"$it\"/>" }
        )
    )
  }
}
