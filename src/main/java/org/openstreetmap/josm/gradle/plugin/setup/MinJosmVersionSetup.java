package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.openstreetmap.josm.gradle.plugin.ProjectKt;
import org.openstreetmap.josm.gradle.plugin.task.AddMinJosmVersionDependency;

public final class MinJosmVersionSetup extends AbstractSetup {

  public MinJosmVersionSetup(final Project project) {
    super(project);
  }

  public void setup() {
    final Task addMinJosmVersionDependency = pro.getTasks().create("addMinJosmVersionDependency", AddMinJosmVersionDependency.class, task -> {
      task.init(pro.getConfigurations().getByName("implementation"));
    });

    pro.afterEvaluate(p -> {
      final SourceSetContainer sourceSets = p.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
      final SourceSet mainSourceSet = sourceSets.getByName("main");
      final SourceSet minJosmVersion = sourceSets.create("minJosmVersion", (sourceSet) -> {
        sourceSet.getJava().setSrcDirs(mainSourceSet.getJava().getSrcDirs());

        sourceSet.resources(resources -> {
          resources.setSrcDirs(mainSourceSet.getResources().getSrcDirs());
          resources.setIncludes(mainSourceSet.getResources().getIncludes());
          resources.setExcludes(mainSourceSet.getResources().getExcludes());
        });

        // Add group/description for minJosmVersionClasses task
        final Task classesTask = p.getTasks().getByName(sourceSet.getClassesTaskName());
        classesTask.setGroup("JOSM");
        classesTask.setDescription("Try to compile against the version of JOSM that is specified in the manifest as the minimum compatible version");

        // Add dependency compileMinJosmVersionJava → addMinJosmVersionDependency
        p.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(addMinJosmVersionDependency);
      });
    });
  }
}
