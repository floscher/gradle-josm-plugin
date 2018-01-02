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
    val arguments: MutableList<String> = if (getProject().hasProperty("josmArgs")) getProject().property("josmArgs").toString().split("\\\\").toMutableList() else ArrayList()
    arguments.add("--load-preferences=" + File(getProject().getBuildDir(), "/josm-custom-config/requiredPlugins.xml").toURI().toURL().toString());

    setGroup("JOSM");
    setMain("org.openstreetmap.josm.gui.MainApplication");
    setArgs(arguments);
    mustRunAfter(getProject().getTasks().getByName("cleanJosm"));
    dependsOn(getProject().getTasks().getByName("updateJosmPlugins"));

    getProject().afterEvaluate{ project ->
      // doFirst has to be added after the project initialized, otherwise it won't be executed before the main part of the JavaExec task is run.
      doFirst{ task ->
        systemProperty("josm.home", task.getProject().getExtensions().getByType(JosmPluginExtension::class.java).tmpJosmHome);
        setClasspath(task.getProject().getConvention().getPlugin(JavaPluginConvention::class.java).getSourceSets().getByName("main").getRuntimeClasspath());

        val L = task.getLogger()

        L.lifecycle("Running version {} of {}", task.getProject().getVersion(), task.getProject().getName());
        L.lifecycle("\nUsing JOSM version {}", task.getProject().getExtensions().getByType(JosmPluginExtension::class.java).josmCompileVersion);

        L.lifecycle("\nThese system properties are set:");
        for (entry in getSystemProperties().entries) {
          L.lifecycle("  {} = {}", entry.key, entry.value);
        }

        if (getArgs().isEmpty()) {
          L.lifecycle("\nNo command line arguments are passed to JOSM.");
        } else {
          L.lifecycle("\nPassing these arguments to JOSM:\n  {}", getArgs().joinToString("\n  "));
        }
        if (!project.hasProperty("josmArgs")) {
          L.lifecycle("\nIf you want to pass additional arguments to JOSM add '-PjosmArgs=\"arg0\\\\arg1\\\\arg2\\\\...\"' when starting Gradle from the commandline (separate the arguments with double-backslashes).")
        }

        L.lifecycle(extraInformation);
        L.lifecycle("\nOutput of JOSM starts with the line after the three equality signs\n===");
      }
    }
  }

}
