package org.openstreetmap.josm.gradle.plugin;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup;
import org.openstreetmap.josm.gradle.plugin.task.TaskSetupKt;

/**
 * Main class of the plugin, sets up the custom configurations <code>requiredPlugin</code> and <code>packIntoJar</code>,
 * the additional repositories and the custom tasks.
 */
public class JosmPlugin implements Plugin<Project> {
  private final SourceDirectorySetFactory sourceDirectorySetFactory;

  @Inject
  public JosmPlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.sourceDirectorySetFactory = sourceDirectorySetFactory;
  }

  /**
   * Set up the JOSM plugin.
   *
   * Creates the tasks this plugin provides, defines the <code>josm</code> extension, adds the repositories where JOSM specific dependencies can be found.
   * Overrides <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/Plugin.html#apply-T-">Plugin.apply()</a>.
   */
  public void apply(@Nonnull final Project project) {
    try {
      project.setVersion(new GitDescriber(project.getProjectDir()).describe(true));
    } catch (Exception e) {
      // Don't set the project version
    }

    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.getPlugins().findPlugin(JavaPlugin.class) == null) {
      project.apply(conf -> conf.plugin(JavaPlugin.class));
    }
    // Define 'josm' extension
    project.getExtensions().create("josm", JosmPluginExtension.class, project);


    final Jar jarTask = project.getTasks().withType(Jar.class).getByName("jar");
    jarTask.doFirst(task -> {
      jarTask.getManifest().attributes(JosmPluginExtension.forProject(project).getManifest().createJosmPluginJarManifest());
      jarTask.from(
        task.getProject().getConfigurations().getByName("packIntoJar").getFiles().stream().map(file ->
          (file.isDirectory()
            ? task.getProject().fileTree(file)
            : task.getProject().zipTree(file)
          ).matching(it -> JosmPluginExtension.forProject(task.getProject()).getPackIntoJarFileFilter().invoke(it))
        ).toArray()
      );
    });

    project.afterEvaluate(p -> {
      // Add the repositories defined in the JOSM configuration
      JosmPluginExtension.forProject(project).getRepositories().invoke(project.getRepositories());
    });

    ConfigurationsSetupKt.setupAsMainConfiguration(project.getConfigurations().getByName("implementation"), project);

    TaskSetupKt.setupJosmTasks(project);
    new PluginTaskSetup(project).setup();

    if (sourceDirectorySetFactory == null) {
      project.getLogger().warn("No source directory set factory given! The i18n source sets are not configured.");
    } else {
      project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(
        s -> SourceSetSetupKt.setup(s, project, sourceDirectorySetFactory)
      );
    }
  }
}
