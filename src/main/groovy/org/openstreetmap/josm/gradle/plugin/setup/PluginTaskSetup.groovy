package org.openstreetmap.josm.gradle.plugin.setup;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.bundling.Jar;

import org.openstreetmap.josm.gradle.plugin.RunJosmTask;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PluginTaskSetup extends AbstractSetup {

  public void setup() {
    final Task writePluginConfig = pro.task("writePluginConfig");
    writePluginConfig.setDescription("Creates the configuration that tells JOSM which plugins to load (which is later automatically loaded by e.g. `runJosm`)");
    writePluginConfig.doFirst{ task ->
      final File customConfig = new File(pro.getBuildDir(), "josm-custom-config/requiredPlugins.xml");
      customConfig.getParentFile().mkdirs();
      task.getLogger().lifecycle("Write required plugins to {}…", customConfig.getAbsolutePath());

      final StringBuilder pluginListEntries = new StringBuilder();
      for (Dependency requiredPlugin : pro.getConfigurations().getByName("requiredPlugin").getDependencies()) {
        pluginListEntries.append("      <entry value=\"").append(requiredPlugin.getName()).append("\"/>\n");
      }
      pluginListEntries.append("      <entry value=\"").append(pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName()).append("\"/>");

      final String xmlTemplate = new BufferedReader(new InputStreamReader(PluginTaskSetup.class.getResourceAsStream("/requiredPluginConfigTemplate.xml"), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(customConfig), StandardCharsets.UTF_8));
      writer.write(xmlTemplate.replace("{{{PLUGIN_LIST_ENTRIES}}}", pluginListEntries.toString()));
      writer.close();
    };

    final Sync updateJosmPlugins = pro.getTasks().withType(Sync.class).getByName("updateJosmPlugins");
    updateJosmPlugins.dependsOn(writePluginConfig);
    updateJosmPlugins.rename("(.*)-\\.jar", "\$1.jar");
    pro.afterEvaluate{ p ->
      updateJosmPlugins.from(pro.getTasks().getByName("dist").getOutputs());
      updateJosmPlugins.from(pro.getConfigurations().getByName("requiredPlugin"));
    };

    final File localDistPath = new File(pro.getBuildDir(), "localDist");
    final File localDistListFile = new File(localDistPath, "list");

    final Task generatePluginList = pro.task("generatePluginList");
    generatePluginList.doFirst{ task ->
      localDistListFile.getParentFile().mkdirs();
      if (localDistListFile.exists()) {
        localDistListFile.delete();
      }

      StringBuilder localDistListBuilder = new StringBuilder();
      // First line containing the name of the plugin and the URL to the *.jar file
      localDistListBuilder
        .append(pro.getTasks().getByName("localDist").property("fileName"))
        .append(';')
        .append(new File(localDistPath, pro.getTasks().getByName("localDist").property("fileName").toString()).toURI().toURL())
        .append('\n');
      // Manifest indented by one tab character
      for (Entry<String, Object> att : pro.getTasks().withType(Jar.class).getByName("jar").getManifest().getEffectiveManifest().getAttributes().entrySet()) {
        // Base64-encode the icon
        if ("Plugin-Icon".equals(att.getKey())) {
          for (final DirectoryTree tree : task.getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main").getResources().getSrcDirTrees()) {
            final File iconFile = new File(tree.getDir(), att.getValue().toString());
            if (iconFile.exists()) {
              String contentType = Files.probeContentType(Paths.get(iconFile.getAbsolutePath()));
              if (contentType == null) {
                final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(iconFile));
                contentType = URLConnection.guessContentTypeFromStream(bis);
                bis.close();
              }
              att.setValue("data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(iconFile.toURI()))));
            }
          }
        }
        // Append date to the plugin version
        if ("Plugin-Version".equals(att.getKey())) {
          att.setValue(att.getValue() + String.format("#%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", new GregorianCalendar()));
        }
        localDistListBuilder.append('\t').append(att.getKey()).append(": ").append(att.getValue()).append('\n');
      }

      final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(localDistListFile), StandardCharsets.UTF_8));
      writer.write(localDistListBuilder.toString());
      writer.close();
    };

    final Sync localDist = pro.getTasks().create("localDist", Sync.class);
    localDist.setGroup("JOSM");
    localDist.setDescription(String.format(
      "Generates a plugin site. Add '%s' as plugin site in JOSM preferences (expert mode) and you'll be able to install the current development state as plugin '%s-dev'.",
      localDistListFile.toURI().toURL(),
      pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName()
    ));
    localDist.getExtensions().getExtraProperties().setProperty("fileName", null);
    localDist.finalizedBy(generatePluginList);
    localDist.from(pro.getTasks().getByName("jar").getOutputs());
    localDist.into(localDistPath);
    localDist.doFirst{ task ->
      task.setProperty("fileName", pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName() + "-dev." + pro.getTasks().withType(Jar.class).getByName("jar").getExtension());
      localDist.rename(".*", task.property("fileName").toString());
    };
    localDist.doLast{ task ->
      task.getLogger().lifecycle("Local JOSM update-site for plugin version {} has been written to {}", task.getProject().getVersion(), localDistListFile.toURI());
    }

    final Sync dist = pro.getTasks().create("dist", Sync.class);
    final File outDir = new File(pro.getBuildDir(), "dist");
    dist.from(pro.getTasks().getByName("jar").getOutputs());
    dist.into(outDir);
    dist.doFirst{ task ->
      dist.rename(
        ".*",
        pro.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName() + '.' + pro.getTasks().withType(Jar.class).getByName("jar").getExtension()
      );
    };
    dist.doLast{ task ->
      task.getLogger().lifecycle("Distribution *.jar (version {}) has ben written into {}", task.getProject().getVersion(), outDir.getAbsolutePath());
    }
    pro.getTasks().getByName("jar").finalizedBy(dist, localDist);
  }
}
