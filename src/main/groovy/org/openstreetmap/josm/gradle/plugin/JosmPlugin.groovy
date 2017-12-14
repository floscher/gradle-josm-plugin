package org.openstreetmap.josm.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.setup.BasicTaskSetup;
import org.openstreetmap.josm.gradle.plugin.setup.I18nTaskSetup;
import org.openstreetmap.josm.gradle.plugin.setup.MinJosmVersionSetup;
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup;

/**
 * Main class of the plugin, sets up the {@code requiredPlugin} configuration,
 * the additional repositories and the custom tasks.
 */
public class JosmPlugin implements Plugin<Project> {
  private static Project currentProject;

  /**
   * The Gradle project to which the gradle-josm-plugin is currently applied
   */
  public static Project getCurrentProject() {
    if (currentProject == null) {
      throw new IllegalStateException("Currently the gradle-josm-plugin is not applied to a Gradle project, but you want to access the project to which the gradle-josm-plugin is applied. This should not happen ;).");
    }
    return currentProject;
  }

  /**
   * Set up the JOSM plugin.
   * Creates the tasks this plugin provides, defines the {@code josm} extension, adds the repositories where JOSM specific dependencies can be found.
   * @see {@link Plugin#apply(T)}
   */
  public synchronized void apply(final Project project) {
    JosmPlugin.currentProject = project;

    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.getPlugins().findPlugin(JavaPlugin.class) == null) {
      project.apply{ it.plugin(JavaPlugin.class)};
    }
    // Define 'josm' extension
    project.getExtensions().create("josm", JosmPluginExtension.class);

    // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
    project.getConfigurations().getByName("implementation").extendsFrom(project.getConfigurations().create("requiredPlugin"));
    // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
    project.getConfigurations().getByName("implementation").extendsFrom(project.getConfigurations().create("packIntoJar"));

    project.repositories(JosmPluginExtension.forProject(project).getRepositories());

    project.getTasks().withType(Jar.class).getByName("jar").doFirst { task ->
      task.getManifest().attributes(JosmPluginExtension.forProject(task.getProject()).getManifest().createJosmPluginJarManifest(task.getProject()));
      task.from(
        task.getProject().getConfigurations().getByName("packIntoJar").getFiles().stream().<FileTree>map({ file ->
          (file.isDirectory()
            ? task.getProject().fileTree(file)
            : task.getProject().zipTree(file)
          ).matching(JosmPluginExtension.forProject(task.getProject()).getPackIntoJarFileFilter())
        }).toArray()
      );
    };

    project.afterEvaluate { p ->
      // Adding dependencies for JOSM and the required plugins
      final Dependency dep = p.getDependencies().add("implementation", "org.openstreetmap.josm:josm:" + JosmPluginExtension.forProject(p).getJosmCompileVersion());
      if ("latest".equals(JosmPluginExtension.forProject(p).getJosmCompileVersion()) || "tested".equals(JosmPluginExtension.forProject(p).getJosmCompileVersion())) {
        p.getLogger().info("Compile against the variable JOSM version " + JosmPluginExtension.forProject(p).getJosmCompileVersion());
        ((ExternalModuleDependency) dep).setChanging(true);
      } else {
        p.getLogger().info("Compile against the JOSM version " + JosmPluginExtension.forProject(p).getJosmCompileVersion());
      }
      try {
        final Set<String> pluginDependencies = JosmPluginExtension.forProject(p).getManifest().getPluginDependencies();
        requirePlugins(p, pluginDependencies.toArray(new String[pluginDependencies.size()]));
      } catch (IOException e) {
        throw new GradleException("Could not determine required JOSM plugins!", e);
      }
    }

    new BasicTaskSetup().setup();
    new I18nTaskSetup().setup();
    new PluginTaskSetup().setup();
    new MinJosmVersionSetup().setup();
    JosmPlugin.currentProject = null;
  }

  /**
   * Convenience method
   */
  private void requirePlugins(Project pro, String... pluginNames) throws IOException {
    requirePlugins(0, pro, pluginNames);
  }
  /**
   * Recursively add the required plugins and any plugins that these in turn require to the configuration `requiredPlugin`.
   * @param recursionDepth starts at 0, each time this method is called from within itself, this is incremented by one
   * @param pro the project, which requires the JOSM plugins given with the last parameter
   * @param pluginNames the names of all required JOSM plugins. Transitive dependencies can be omitted, these will be filled in by this method.
   * @throws GradleException if the parameter {@code recursionDepth} reaches the current limit of 10
   */
  private void requirePlugins(final int recursionDepth, final Project pro, final String... pluginNames) throws IOException {
    if (recursionDepth >= pro.getExtensions().getByType(JosmPluginExtension.class).getMaxPluginDependencyDepth()) {
      throw new GradleException(String.format("Dependency tree of required JOSM plugins is too deep (>= %d steps). Aborting resolution of required JOSM plugins.", pro.getExtensions().getByType(JosmPluginExtension.class).getMaxPluginDependencyDepth()));
    }
    final String indention = String.join("", Collections.nCopies(recursionDepth, "  "));
    for (String pluginName : pluginNames) {
      pluginName = pluginName.trim();
      pro.getLogger().info("{}Add required JOSM plugin '{}' to classpath…", indention, pluginName);

      final Configuration tmpConf = pro.getConfigurations().create("tmpConf"+recursionDepth);
      final Dependency pluginDep = pro.getDependencies().add("tmpConf"+recursionDepth, "org.openstreetmap.josm.plugins:" + pluginName + ':');
      ((ExternalModuleDependency) pluginDep).setChanging(true);
      if (pro.getConfigurations().getByName("requiredPlugin").getDependencies().contains(pluginDep)) {
        pro.getLogger().info("{}JOSM plugin '{}' is already on the classpath.", indention, pluginName);
      } else {
        for (File jarFile : tmpConf.fileCollection(pluginDep).getFiles()) {
          final ZipFile zipFile = new ZipFile(jarFile);
          final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
          while (zipEntries.hasMoreElements()) {
            final ZipEntry zipEntry = zipEntries.nextElement();
            if ("META-INF/MANIFEST.MF".equals(zipEntry.getName())) {
              final String requirements = new Manifest(zipFile.getInputStream(zipEntry)).getMainAttributes().getValue("Plugin-Requires");
              if (requirements != null) {
                // If the plugin itself requires more plugins, recursively add them too.
                requirePlugins(Math.max(1, recursionDepth + 1), pro, requirements.split(";"));
              }
            }
          }
        }
        pro.getConfigurations().getByName("requiredPlugin").getDependencies().add(pluginDep);
      }
      pro.getConfigurations().remove(tmpConf);
    }
  }
}
