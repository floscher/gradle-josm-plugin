package org.openstreetmap.josm.gradle.plugin.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.TaskInstantiationException;
import org.gradle.api.tasks.bundling.Jar;
import org.openstreetmap.josm.gradle.plugin.ProjectKt;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;
import org.openstreetmap.josm.gradle.plugin.task.GeneratePluginList;
import org.openstreetmap.josm.gradle.plugin.task.LangCompile;

public class PluginTaskSetup extends AbstractSetup {

  public PluginTaskSetup(final Project project) {
    super(project);
  }

  public void setup() {
    final Task writePluginConfig = pro.task("writePluginConfig");
    writePluginConfig.setDescription("Creates the configuration that tells JOSM which plugins to load (which is later automatically loaded by e.g. `runJosm`)");
    writePluginConfig.doFirst(task -> {
      final File customConfig = new File(pro.getBuildDir(), "josm-custom-config/requiredPlugins.xml");
      customConfig.getParentFile().mkdirs();
      task.getLogger().lifecycle("Write required plugins to {}…", customConfig.getAbsolutePath());

      final StringBuilder pluginListEntries = new StringBuilder();
      for (Dependency requiredPlugin : pro.getConfigurations().getByName("requiredPlugin").getDependencies()) {
        pluginListEntries.append("      <entry value=\"").append(requiredPlugin.getName()).append("\"/>\n");
      }
      pluginListEntries.append("      <entry value=\"").append(pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName()).append("\"/>");

      final String xmlTemplate = new BufferedReader(new InputStreamReader(PluginTaskSetup.class.getResourceAsStream("/requiredPluginConfigTemplate.xml"), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(customConfig), StandardCharsets.UTF_8))) {
        writer.write(xmlTemplate.replace("{{{PLUGIN_LIST_ENTRIES}}}", pluginListEntries.toString()).replace("{{{tmpJosmPrefDir}}}", JosmPluginExtension.forProject(pro).getTmpJosmPrefDir().getAbsolutePath()));
      } catch (IOException e) {
        throw new TaskExecutionException(task, e);
      }
    });

    final Sync updateJosmPlugins = pro.getTasks().withType(Sync.class).getByName("updateJosmPlugins");
    updateJosmPlugins.dependsOn(writePluginConfig);
    updateJosmPlugins.rename("(.*)-\\.jar", "$1.jar");
    pro.afterEvaluate(p -> {
      updateJosmPlugins.from(pro.getTasks().getByName("dist").getOutputs());
      updateJosmPlugins.from(pro.getConfigurations().getByName("requiredPlugin"));
    });

    final File localDistPath = new File(pro.getBuildDir(), "localDist");
    final File localDistListFile = new File(localDistPath, "list");

    final Task generatePluginList = pro.getTasks().create("generatePluginList", GeneratePluginList.class, task -> {
      pro.afterEvaluate(p -> {
        final LangCompile langCompile = JosmPluginExtension.forProject(pro).getManifest().getLangCompileTask();
        if (langCompile != null) {
          task.dependsOn(langCompile);
        }
      });
      task.doFirst(t -> {
        try {
          task.addPlugin(
            getLocalDistFileName(pro),
            JosmPluginExtension.forProject(pro).getManifest().createJosmPluginJarManifest(),
            new File(localDistPath, getLocalDistFileName(pro)).toURI().toURL()
          );
        } catch (MalformedURLException e) {
          throw new TaskInstantiationException("The URL to the local distribution is malformed!", e);
        }
      });
      task.setOutputFile(localDistListFile);
      task.setIconBase64Provider(iconPath -> {
        try {
          final Optional<File> iconFile = ProjectKt.getJava(pro.getConvention()).getSourceSets().getByName("main").getResources().getSrcDirs().stream().map(srcDir -> new File(srcDir, iconPath)).filter(File::exists).findAny();
          if (iconFile.isPresent()) {
            String contentType = Files.probeContentType(Paths.get(iconFile.get().toURI()));
            if (contentType == null) {
              final InputStream is = new FileInputStream(iconFile.get());
              contentType = URLConnection.guessContentTypeFromStream(is);
              is.close();
            }
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(iconFile.get().toURI())));
          }
        } catch (IOException e) {
          task.getLogger().lifecycle("Error reading icon file!", e);
        }
        return null;
      });
    });

    final Sync localDist = pro.getTasks().create("localDist", Sync.class);
    localDist.setGroup("JOSM");
    try {
      localDist.setDescription(String.format(
        "Generates a plugin site. Add '%s' as plugin site in JOSM preferences (expert mode) and you'll be able to install the current development state as plugin '%s-dev'.",
        localDistListFile.toURI().toURL(),
        pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName()
      ));
    } catch (MalformedURLException e) {
      pro.getLogger().error("URL to local update-site seems to be malformed!", e);
      localDist.setDescription("Generates a plugin site.");
    }
    localDist.finalizedBy(generatePluginList);
    localDist.from(pro.getTasks().getByName("jar").getOutputs());
    localDist.into(localDistPath);
    localDist.doFirst(task -> {
      localDist.rename(".*", getLocalDistFileName(pro));
    });
    localDist.doLast(task -> {
      task.getLogger().lifecycle("Local JOSM update-site for plugin version {} has been written to {}", task.getProject().getVersion(), localDistListFile.toURI());
    });

    final Sync dist = pro.getTasks().create("dist", Sync.class);
    final File outDir = new File(pro.getBuildDir(), "dist");
    dist.from(pro.getTasks().getByName("jar").getOutputs());
    dist.into(outDir);
    dist.doFirst(task -> {
      dist.rename(
        ".*",
        pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName() + '.' + pro.getTasks().withType(Jar.class).getByName("jar").getExtension()
      );
    });
    dist.doLast(task -> {
      task.getLogger().lifecycle("Distribution *.jar (version {}) has ben written into {}", task.getProject().getVersion(), outDir.getAbsolutePath());
    });
    pro.getTasks().getByName("jar").finalizedBy(dist, localDist);
  }

  private String getLocalDistFileName(final Project p) {
    return p.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName() + "-dev." + p.getTasks().withType(Jar.class).getByName("jar").getExtension();
  }
}
