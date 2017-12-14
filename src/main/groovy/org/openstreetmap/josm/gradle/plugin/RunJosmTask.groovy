package org.openstreetmap.josm.gradle.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;

/**
 * A task that can execute a JOSM instance. Both the {@code runJosm} task and the {@code debugJosm} task extend this type of task.
 */
public class RunJosmTask extends JavaExec {
  /**
   * Text that should be displayed in the console output right before JOSM is started up. Defaults to the empty string.
   */
  private String extraInformation = "";
  /**
   * Instantiates a new task for running a JOSM instance.
   * By default the source set <code>main</code> is added to
   */
  public RunJosmTask() throws MalformedURLException {
    List<String> arguments = getProject().hasProperty("josmArgs") ? Arrays.asList(getProject().property("josmArgs").toString().split("\\\\")) : new ArrayList<>();
    arguments.add("--load-preferences=" + new File(JosmPlugin.getCurrentProject().getBuildDir(), "/josm-custom-config/requiredPlugins.xml").toURI().toURL().toString());

    setGroup("JOSM");
    setMain("org.openstreetmap.josm.gui.MainApplication");
    setArgs(arguments);
    mustRunAfter(getProject().getTasks().getByName("cleanJosm"));
    dependsOn(getProject().getTasks().getByName("updateJosmPlugins"));

    getProject().afterEvaluate{ project ->
      // doFirst has to be added after the project initialized, otherwise it won't be executed before the main part of the JavaExec task is run.
      doFirst{ task ->
        systemProperty("josm.home", task.getProject().getExtensions().getByType(JosmPluginExtension.class).getTmpJosmHome());
        setClasspath(task.getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main").getRuntimeClasspath());

        final Logger L = task.getLogger();

        L.lifecycle("Running version {} of {}", task.getProject().getVersion(), task.getProject().getName());
        L.lifecycle("\nUsing JOSM version {}", task.getProject().getExtensions().getByType(JosmPluginExtension.class).getJosmCompileVersion());

        L.lifecycle("\nThese system properties are set:");
        for (Entry<String, Object> entry : getSystemProperties().entrySet()) {
          L.lifecycle(entry.getKey() + " = " + entry.getValue());
        }

        if (getArgs().isEmpty()) {
          L.lifecycle("\nNo command line arguments are passed to JOSM.\nIf you want to pass arguments to JOSM add '-PjosmArgs=\"arg0\\\\arg1\\\\arg2\\\\...\"' when starting Gradle from the commandline (separate the arguments with double-backslashes).");
        } else {
          L.lifecycle("\nPassing these arguments to JOSM:\n" + String.join("\n", getArgs()));
        }
        L.lifecycle(getExtraInformation());
        L.lifecycle("\nOutput of JOSM starts with the line after the three equality signs\n===");
      };
    }
  }

  /**
   * Set the text that should be displayed right before the console output of JOSM when starting JOSM.
   * This is used e.g. to display the remote debugging port.
   */
  public void setExtraInformation(final String extraInformation) {
    this.extraInformation = extraInformation;
  }

  /**
   * Getter method for the field {@code extraInformation}
   */
  @Internal(value = "Only for informational purposes, message is only displayed during execution.")
  public String getExtraInformation() {
    return this.extraInformation;
  }

}
