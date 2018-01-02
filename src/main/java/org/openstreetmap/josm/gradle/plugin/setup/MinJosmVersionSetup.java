package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.openstreetmap.josm.gradle.plugin.ProjectKt;

public final class MinJosmVersionSetup extends AbstractSetup {

  public MinJosmVersionSetup(final Project project) {
    super(project);
  }

  public void setup() {
    final Configuration minJosmVersionImplementation = pro.getConfigurations().create("minJosmVersionImplementation").extendsFrom(pro.getConfigurations().getByName("implementation"));

    final Task addMinJosmVersionDependency = pro.task("addMinJosmVersionDependency");
    addMinJosmVersionDependency.setDescription("Adds dependency for the minimum required JOSM version to the configuration `minJosmVersionImplementation`.");
    addMinJosmVersionDependency.doFirst(task -> {
      // Find the next available version from the one specified in the manifest
      task.getProject().getDependencies().add(
        minJosmVersionImplementation.getName(),
        ProjectKt.getNextJosmVersion(pro, ProjectKt.getJosmExtension(task.getProject()).getManifest().getMinJosmVersion())
      );
    });

    pro.afterEvaluate(p -> {
      final SourceSetContainer sourceSets = p.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
      final SourceSet mainSourceSet = sourceSets.getByName("main");
      final SourceSet minJosmVersion = sourceSets.create("minJosmVersion");
      minJosmVersion.getJava().setSrcDirs(mainSourceSet.getJava().getSrcDirs());
      minJosmVersion.resources(resources -> {
        resources.setSrcDirs(mainSourceSet.getResources().getSrcDirs());
        resources.setIncludes(mainSourceSet.getResources().getIncludes());
      });

      p.getTasks().getByName("minJosmVersionClasses").setGroup("JOSM");
      p.getTasks().getByName("minJosmVersionClasses").setDescription("Try to compile against the version of JOSM that is specified in the manifest as the minimum compatible version");
      p.getTasks().getByName("compileMinJosmVersionJava").dependsOn(addMinJosmVersionDependency);
    });
  }
}
