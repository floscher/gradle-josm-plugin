package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import java.io.File

/**
 * A task that can execute a JOSM instance. Both the {@code runJosm} task and the `debugJosm` task extend this type of task.
 *
 * @constructor
 * Instantiates a new task for running a JOSM instance.
 *
 * By default the source set `main` is added to the classpath.
 */
open class RunJosmTask : JavaExec() {

  /**
   * Text that should be displayed in the console output right before JOSM is started up. Defaults to the empty string.
   *
   * This is used e.g. to display the remote debugging port for task `debugJosm`.
   */
  @Internal
  var extraInformation: String = ""


  init {
    val arguments: MutableList<String> = if (project.hasProperty("josmArgs")) project.property("josmArgs").toString().split("\\\\").toMutableList() else ArrayList()
    arguments.add("--load-preferences=" + File(project.buildDir, "/josm-custom-config/requiredPlugins.xml").toURI().toURL().toString());

    group = "JOSM"
    main = "org.openstreetmap.josm.gui.MainApplication"
    args = arguments
    super.mustRunAfter(project.tasks.getByName("cleanJosm"));
    super.dependsOn(project.tasks.getByName("updateJosmPlugins"));

    project.afterEvaluate{
      // doFirst has to be added after the project initialized, otherwise it won't be executed before the main part of the JavaExec task is run.
      doFirst{
        systemProperty("josm.home", project.extensions.getByType(JosmPluginExtension::class.java).tmpJosmHome);
        classpath = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main").runtimeClasspath

        logger.lifecycle("Running version {} of {}", project.version, project.name);
        logger.lifecycle("\nUsing JOSM version {}", project.extensions.getByType(JosmPluginExtension::class.java).josmCompileVersion);

        logger.lifecycle("\nThese system properties are set:");
        for ((key, value) in systemProperties) {
          logger.lifecycle("  {} = {}", key, value);
        }

        if (args.isEmpty()) {
          logger.lifecycle("\nNo command line arguments are passed to JOSM.");
        } else {
          logger.lifecycle("\nPassing these arguments to JOSM:\n  {}", args.joinToString("\n  "));
        }
        if (!project.hasProperty("josmArgs")) {
          logger.lifecycle("\nIf you want to pass additional arguments to JOSM add '-PjosmArgs=\"arg0\\\\arg1\\\\arg2\\\\...\"' when starting Gradle from the commandline (separate the arguments with double-backslashes).")
        }

        logger.lifecycle(extraInformation);
        logger.lifecycle("\nOutput of JOSM starts with the line after the three equality signs\n===");
      }
    }
  }
}
