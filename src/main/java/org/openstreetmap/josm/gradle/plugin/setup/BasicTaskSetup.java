package org.openstreetmap.josm.gradle.plugin.setup;

import java.io.File;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.task.CleanJosm;
import org.openstreetmap.josm.gradle.plugin.task.RunJosmTask;

public class BasicTaskSetup extends AbstractSetup {

  public BasicTaskSetup(final Project project) {
    super(project);
  }

  public void setup() {

    // Clean JOSM
    final Delete cleanJosm = pro.getTasks().create("cleanJosm", CleanJosm.class);

    // Init JOSM preferences.xml file
    final Copy initJosmPrefs = pro.getTasks().create("initJosmPrefs", Copy.class);
    initJosmPrefs.setDescription("Puts a default preferences.xml file into the temporary JOSM home directory");
    initJosmPrefs.include("preferences.xml");
    pro.afterEvaluate(p -> {
      initJosmPrefs.from(JosmPluginExtension.forProject(p).getJosmConfigDir());
      initJosmPrefs.into(JosmPluginExtension.forProject(p).getTmpJosmHome());
      if (initJosmPrefs.getSource().isEmpty()) {
        initJosmPrefs.getLogger().debug("No default JOSM preference file found in {}/preferences.xml.", JosmPluginExtension.forProject(p).getJosmConfigDir().getAbsolutePath());
      }
    });
    initJosmPrefs.doFirst(task -> {
      if (new File(initJosmPrefs.getDestinationDir(), "preferences.xml").exists()) {
        task.getLogger().lifecycle("JOSM preferences not copied, file is already present.\nIf you want to replace it, run the task 'cleanJosm' additionally.");
      } else {
        task.getLogger().lifecycle("Copy [{}] to {}…", String.join(", ", initJosmPrefs.getSource().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList())), initJosmPrefs.getDestinationDir().getAbsolutePath());
      }
    });

    // Copy all needed JOSM plugin *.jar files into the directory in {@code $JOSM_HOME}
    final Sync updateJosmPlugins = pro.getTasks().create("updateJosmPlugins", Sync.class);
    updateJosmPlugins.setDescription("Put all needed plugin *.jar files into the plugins directory. This task copies files into the temporary JOSM home directory.");
    updateJosmPlugins.dependsOn(initJosmPrefs);
    pro.afterEvaluate(p -> {
      updateJosmPlugins.into(new File(JosmPluginExtension.forProject(p).getTmpJosmHome(), "plugins"));
      // the rest of the configuration (e.g. from where the files come, that should be copied) is done later (e.g. in the file `PluginTaskSetup.java`)
    });

    // Standard run-task
    final Task runJosm = pro.getTasks().create("runJosm", RunJosmTask.class);
    runJosm.setDescription("Runs an independent JOSM instance (version specified in project dependencies) with `build/.josm/` as home directory and the freshly compiled plugin active.");

    // Debug task
    final RunJosmTask debugJosm = pro.getTasks().create("debugJosm", RunJosmTask.class, task -> {
      task.getProject().afterEvaluate(p -> {
        final Integer debugPort = JosmPluginExtension.forProject(p).getDebugPort();
        task.setDescription("Runs a JOSM instance like the task `runJosm`, but with JDWP (Java debug wire protocol) active" + (
          debugPort == null
            ? ".\n  NOTE: Currently the `debugJosm` task will error out! Set the property `project.josm.debugPort` to enable it!"
            : " on port " + debugPort
        ));
      });
      task.doFirst(taskA -> {
        final Integer debugPort = JosmPluginExtension.forProject(task.getProject()).getDebugPort();
        if (debugPort == null) {
          throw new TaskExecutionException(task, new NullPointerException(
            "You have to set the property `project.josm.debugPort` to the port on which you'll listen for debug output. If you don't want to debug, simply use the task `runJosm` instead of `debugJosm`."
          ));
        }
        task.setExtraInformation(
          "\nThe application is listening for a remote debugging connection on port " +
            debugPort + ". It will start execution as soon as the debugger is connected.\n"
        );
        task.jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
      });
    });
  }
}
