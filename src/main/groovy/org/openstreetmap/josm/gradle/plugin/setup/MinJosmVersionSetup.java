package org.openstreetmap.josm.gradle.plugin.setup;

import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class MinJosmVersionSetup extends AbstractSetup {

  public void setup() {
    pro.getConfigurations().create("minJosmVersionImplementation").extendsFrom(pro.getConfigurations().getByName("implementation"));

    final Task addMinJosmVersionDependency = pro.task("addMinJosmVersionDependency");
    addMinJosmVersionDependency.setDescription("Adds dependency for the minimum required JOSM version to the configuration `minJosmVersionImplementation`.");
    addMinJosmVersionDependency.doFirst(task -> {
      // Find the next available version from the one specified in the manifest
      final Integer minJosmVersion = getNextJosmVersion(JosmPluginExtension.forProject(task.getProject()).getManifest().getMinJosmVersion());
      if (minJosmVersion == null) {
        throw new GradleException("Could not determine the minimum required JOSM version from the given version number '" + JosmPluginExtension.forProject(pro).getManifest().getMinJosmVersion() + "'");
      }
      task.getLogger().lifecycle("Use JOSM version {} for compiling against the minimum required version", minJosmVersion);
      task.getProject().getDependencies().add("minJosmVersionImplementation", "org.openstreetmap.josm:josm:"+ minJosmVersion);
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

  /**
   * Returns the next JOSM version available for download for a version number given as string
   */
  private Integer getNextJosmVersion(final String startVersionString) {
    final int startVersion = Integer.parseInt(startVersionString);
    for (int i = startVersion; i < startVersion + 50; i++) {
      pro.getLogger().lifecycle("Checking if JOSM version {} is available for download…", i);
      try {
        URL u1 = new URL("https://josm.openstreetmap.de/download/josm-snapshot-" + i + ".jar");
        URL u2 = new URL("https://josm.openstreetmap.de/download/Archiv/josm-snapshot-" + i + ".jar");
        HttpURLConnection con1 = (HttpURLConnection) u1.openConnection();
        con1.setRequestMethod("HEAD");
        HttpURLConnection con2 = (HttpURLConnection) u2.openConnection();
        con2.setRequestMethod("HEAD");
        if (con1.getResponseCode() == 200 || con2.getResponseCode() == 200) {
          return i;
        }
      } catch(IOException e) {
        pro.getLogger().error("An exception occurred when searching for the next JOSM version available after " + startVersionString, e);
      }
    }
    return null;
  }
}
