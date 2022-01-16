package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.util.java
import org.openstreetmap.josm.gradle.plugin.util.josm
import javax.inject.Inject

/**
 * A task that can execute a JOSM instance. There's also the class [DebugJosm], which extends this class and allows to
 * remote debug via JDWP (Java debug wire protocol).
 *
 * @constructor
 * Instantiates a new task for running a JOSM instance.
 *
 * By default the source set `main` is added to the classpath.
 */
open class RunJosmTask @Inject constructor(cleanTask: Provider<out CleanJosm>, @get:InputFiles val initPrefTask: Provider<out InitJosmPreferences>) : JavaExec() {

  /**
   * Text that should be displayed in the console output right before JOSM is started up. Defaults to the empty string.
   *
   * This is used e.g. to display the remote debugging port for task `debugJosm`.
   */
  @Internal
  var extraInformation: String = ""

  @Internal
  override fun getDescription() = "Run an independent clean JOSM instance (v${project.extensions.josm.josmCompileVersion}) with temporary JOSM home directories (by default inside `build/.josm/`) and the freshly compiled plugin active."
  override fun setDescription(description: String?) = throw UnsupportedOperationException("Description must not be modified!")

  init {
    group = "JOSM"
    mainClass.set("org.openstreetmap.josm.gui.MainApplication")
    super.mustRunAfter(cleanTask)
  }

  @TaskAction
  override fun exec() {
    val userSuppliedArgs = args ?: listOf()
    val allArgs = userSuppliedArgs.plus("--load-preferences=${initPrefTask.get().preferencesInitFile.get().asFile.toURI().toURL()}")
    this.args = allArgs

    if (project.extensions.josm.useSeparateTmpJosmDirs()) {
      systemProperty("josm.cache", project.extensions.josm.tmpJosmCacheDir)
      systemProperty("josm.pref", project.extensions.josm.tmpJosmPrefDir)
      systemProperty("josm.userdata", project.extensions.josm.tmpJosmUserdataDir)
    } else {
      systemProperty("josm.home", project.extensions.josm.tmpJosmPrefDir)
    }
    classpath = project.extensions.java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath

    logger.lifecycle("Running version ${project.extensions.josm.josmCompileVersion} of JOSM with version ${project.version} of plugin '${project.name}' …")

    logger.lifecycle(
      "These system properties are set:" + (
        systemProperties
          .takeIf { it.isNotEmpty() }
          ?.map { (key, value) -> "  $key = $value" }
          ?.joinToString("\n", prefix = "\n")
          ?: " no system properties set"
      )
    )

    logger.lifecycle("")

    logger.lifecycle(
      if (allArgs.isEmpty()) {
        "No command line arguments are passed to JOSM."
      } else {
        "Passing ${if (allArgs.size <= 1) "this argument" else "these ${allArgs.size} arguments"} to JOSM:\n  ${allArgs.joinToString("\n  ")}"
      }
    )
    if (userSuppliedArgs.isEmpty()) {
      logger.lifecycle("""If you want to pass additional arguments to JOSM add something like the following when starting Gradle from the commandline: --args='--debug --language="es"'""")
    }

    logger.lifecycle(extraInformation)
    logger.lifecycle("\nOutput of JOSM starts with the line below the three equality signs\n===")

    super.exec()
  }
}
