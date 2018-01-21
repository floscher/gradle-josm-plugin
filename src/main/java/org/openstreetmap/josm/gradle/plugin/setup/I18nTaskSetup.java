package org.openstreetmap.josm.gradle.plugin.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.TaskExecutionException;
import org.openstreetmap.josm.gradle.plugin.task.GeneratePot;
import org.openstreetmap.josm.gradle.plugin.task.TransifexDownload;

public class I18nTaskSetup extends AbstractSetup {

  public I18nTaskSetup(final Project project) {
    super(project);
  }

  private Set<File> getMainJavaSrcDirs(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main").getJava().getSrcDirs();
  }

  public void setup() {
    // Generate a list of all files in the main Java source set
    final Task genSrcFileList = pro.task("generateSrcFileList");
    final File outFile = new File(pro.getBuildDir(), "srcFileList.txt");
    genSrcFileList.getOutputs().file(outFile);
    pro.afterEvaluate( p -> {
      genSrcFileList.getInputs().files(getMainJavaSrcDirs(pro));
    });
    genSrcFileList.doFirst(task -> {
      outFile.delete();
      outFile.getParentFile().mkdirs();
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
      for (File dir : getMainJavaSrcDirs(pro)) {
        Files.walk(Paths.get(dir.toURI()))
          .filter(Files::isRegularFile)
          .forEach(f -> {
            try {
              writer.write(f.toAbsolutePath() + "\n");
            } catch (IOException e) {
              throw new TaskExecutionException(task, e);
            }
          });
      }
      } catch (IOException e) {
        throw new TaskExecutionException(task, e);
      }
    });

    pro.getTasks().create("generatePot", GeneratePot.class, task -> {
      task.dependsOn(genSrcFileList);
      task.getInputs().files(getMainJavaSrcDirs(pro));
    });

    pro.getTasks().create("transifexDownload", TransifexDownload.class);
  }
}
