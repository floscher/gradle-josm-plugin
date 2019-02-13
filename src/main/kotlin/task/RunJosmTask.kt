package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.openstreetmap.josm.gradle.plugin.josm
import org.openstreetmap.josm.gradle.plugin.useSeparateTmpJosmDirs
import java.io.File
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
open class RunJosmTask @Inject constructor(prefFile: File) : JavaExec() {

  /**
   * Text that should be displayed in the console output right before JOSM is started up. Defaults to the empty string.
   *
   * This is used e.g. to display the remote debugging port for task `debugJosm`.
   */
  @Internal
  var extraInformation: String = ""

  init {
    group = "JOSM"
    main = "org.openstreetmap.josm.gui.MainApplication"
    super.mustRunAfter(project.tasks.getByName("cleanJosm"))
    super.dependsOn(project.tasks.getByName("updateJosmPlugins"))

    project.afterEvaluate{ project ->
      description = "Runs an independent clean JOSM instance (v${project.extensions.josm.josmCompileVersion}) with temporary JOSM home directories (by default inside `build/.josm/`) and the freshly compiled plugin active."
      // doFirst has to be added after the project initialized, otherwise it won't be executed before the main part of the JavaExec task is run.
      doFirst{
        val userSuppliedArgs = args ?: listOf()
        this.args = userSuppliedArgs.plus("""--load-preferences=${prefFile.toURI().toURL()}""")

        if (project.useSeparateTmpJosmDirs()) {
          systemProperty("josm.cache", project.extensions.josm.tmpJosmCacheDir)
          systemProperty("josm.pref", project.extensions.josm.tmpJosmPrefDir)
          systemProperty("josm.userdata", project.extensions.josm.tmpJosmUserdataDir)
        } else {
          systemProperty("josm.home", project.extensions.josm.tmpJosmPrefDir)
        }
        classpath = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main").runtimeClasspath

        logger.lifecycle("Running version {} of {}", project.version, project.name)
        logger.lifecycle("\nUsing JOSM version {}", project.extensions.josm.josmCompileVersion)

        logger.lifecycle("\nThese system properties are set:")
        for ((key, value) in systemProperties) {
          logger.lifecycle("  {} = {}", key, value)
        }

        val args = args ?: listOf() // make field args final and non-null
        if (args.isEmpty()) {
          logger.lifecycle("\nNo command line arguments are passed to JOSM.")
        } else {
          logger.lifecycle("\nPassing these arguments to JOSM:\n  {}", args.joinToString("\n  "))
        }
        if (userSuppliedArgs.isEmpty()) {
          logger.lifecycle("""\nIf you want to pass additional arguments to JOSM add something like the following when starting Gradle from the commandline: --args='--debug --language="es"'""")
        }

        logger.lifecycle(extraInformation)
        logger.lifecycle("\nOutput of JOSM starts with the line after the three equality signs\n===")
      }
    }
  }
}
