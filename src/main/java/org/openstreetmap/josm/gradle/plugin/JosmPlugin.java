package org.openstreetmap.josm.gradle.plugin;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.i18n.DefaultI18nSourceSet;
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup;
import org.openstreetmap.josm.gradle.plugin.task.LangCompile;
import org.openstreetmap.josm.gradle.plugin.task.ListJosmVersions;
import org.openstreetmap.josm.gradle.plugin.task.MoCompile;
import org.openstreetmap.josm.gradle.plugin.task.PoCompile;
import org.openstreetmap.josm.gradle.plugin.task.ShortenPoFiles;
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
    project.getTasks().create("listJosmVersions", ListJosmVersions.class);

    if (sourceDirectorySetFactory == null) {
      project.getLogger().warn("No source directory set factory given! The i18n source sets are not configured.");
    } else {
      // Inspired by https://github.com/gradle/gradle/blob/9d86f98b01acb6496d05e05deddbc88c1e35d038/subprojects/plugins/src/main/java/org/gradle/api/plugins/GroovyBasePlugin.java#L88-L113
      project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(s -> {
        if (!"minJosmVersion".equals(s.getName()) && !s.getName().isEmpty()) {
          final DefaultI18nSourceSet i18nSourceSet = new DefaultI18nSourceSet(s, sourceDirectorySetFactory);
          new DslObject(s).getConvention().getPlugins().put("i18n", i18nSourceSet);
          project.getTasks().create(
            "main".equals(s.getName()) ? "shortenPoFiles" : "shorten" + s.getName().substring(0, 1).toUpperCase(Locale.UK) + s.getName().substring(1) + "PoFiles",
            ShortenPoFiles.class,
            t -> t.setSourceSet(i18nSourceSet)
          );
          final PoCompile poCompileTask = project.getTasks().create(s.getCompileTaskName("po"), PoCompile.class, t -> t.setSourceSet(i18nSourceSet));
          final MoCompile moCompileTask = project.getTasks().create(s.getCompileTaskName("mo"), MoCompile.class, t -> {
            t.setSourceSet(i18nSourceSet);
            t.setPoCompile(poCompileTask);
          });
          final LangCompile langCompileTask = project.getTasks().create(s.getCompileTaskName("lang"), LangCompile.class, t -> {
            t.setSourceSet(i18nSourceSet);
            t.setMoCompile(moCompileTask);
          });

          s.getOutput().dir(langCompileTask);

        }
      });
    }
  }
}
