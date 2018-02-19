package org.openstreetmap.josm.gradle.plugin;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.i18n.DefaultI18nSourceSet;
import org.openstreetmap.josm.gradle.plugin.setup.BasicTaskSetup;
import org.openstreetmap.josm.gradle.plugin.setup.I18nTaskSetup;
import org.openstreetmap.josm.gradle.plugin.setup.MinJosmVersionSetup;
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup;
import org.openstreetmap.josm.gradle.plugin.task.MoCompile;
import org.openstreetmap.josm.gradle.plugin.task.PoCompile;
import org.openstreetmap.josm.gradle.plugin.task.ShortenPoFiles;

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

    // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
    project.getConfigurations().getByName("implementation").extendsFrom(project.getConfigurations().create("requiredPlugin"));
    // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
    project.getConfigurations().getByName("implementation").extendsFrom(project.getConfigurations().create("packIntoJar"));

    JosmPluginExtension.forProject(project).getRepositories().invoke(project.getRepositories());

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
      // Adding dependencies for JOSM and the required plugins
      final Dependency dep = p.getDependencies().add("implementation", "org.openstreetmap.josm:josm:" + JosmPluginExtension.forProject(p).getJosmCompileVersion());
      if ("latest".equals(JosmPluginExtension.forProject(p).getJosmCompileVersion()) || "tested".equals(JosmPluginExtension.forProject(p).getJosmCompileVersion())) {
        p.getLogger().info("Compile against the variable JOSM version " + JosmPluginExtension.forProject(p).getJosmCompileVersion());
        ((ExternalModuleDependency) dep).setChanging(true);
      } else {
        p.getLogger().info("Compile against the JOSM version " + JosmPluginExtension.forProject(p).getJosmCompileVersion());
      }
      ProjectKt.getAllRequiredJosmPlugins(p, JosmPluginExtension.forProject(p).getManifest().getPluginDependencies()).forEach(it -> p.getDependencies().add("requiredPlugin", it));
    });

    new BasicTaskSetup(project).setup();
    new I18nTaskSetup(project).setup();
    new PluginTaskSetup(project).setup();
    new MinJosmVersionSetup(project).setup();

    if (sourceDirectorySetFactory == null) {
      project.getLogger().lifecycle("No source directory set factory given! The i18n source sets are not configured.");
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
          final PoCompile poCompileTask = project.getTasks().create(s.getCompileTaskName("po"), PoCompile.class, t -> t.setup(i18nSourceSet));
          final MoCompile moCompileTask = project.getTasks().create(s.getCompileTaskName("mo"), MoCompile.class, t -> t.setup(i18nSourceSet, poCompileTask));

          s.getOutput().dir(moCompileTask.getOutDir());
          i18nSourceSet.getLang().getSourceDirectories().forEach(sd -> s.getOutput().dir(sd));
          project.getTasks().getByName(s.getProcessResourcesTaskName()).getInputs().files(moCompileTask);
        }
      });
    }
  }
}
