package org.openstreetmap.josm.gradle.plugin.setup;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.openstreetmap.josm.gradle.plugin.task.GeneratePot;
import org.openstreetmap.josm.gradle.plugin.task.TransifexDownload;
import task.GenerateFileList;

public class I18nTaskSetup extends AbstractSetup {

  public I18nTaskSetup(final Project project) {
    super(project);
  }

  private SourceSet getMainSourceSet() {
    return pro.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
  }

  public void setup() {
    // Generate a list of all files in the main Java source set
    final GenerateFileList genSrcFileList = pro.getTasks().create("generateSrcFileList", GenerateFileList.class, task -> {
      task.setOutFile(new File(pro.getBuildDir(), "srcFileList.txt"));
      task.setSrcSet(getMainSourceSet());
    });

    pro.getTasks().create("generatePot", GeneratePot.class, task -> {
      task.setFileListGenTask(genSrcFileList);
    });

    pro.getTasks().create("transifexDownload", TransifexDownload.class);
  }
}
