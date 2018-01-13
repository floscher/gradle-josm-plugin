package org.openstreetmap.josm.gradle.plugin.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskExecutionException;
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension;

public class I18nTaskSetup extends AbstractSetup {

  public I18nTaskSetup(final Project project) {
    super(project);
  }

  private Set<File> getMainJavaSrcDirs(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main").getJava().getSrcDirs();
  }

  public void setup() {
    // Generate a list of all files in the main Java source set
    final Task genSrcFileList = pro.task("genSrcFileList");
    final File outFile = new File(pro.getBuildDir(), "srcFileList.txt");
    genSrcFileList.getOutputs().file(outFile);
    pro.afterEvaluate( p -> {
      genSrcFileList.getInputs().files(getMainJavaSrcDirs(pro));
    });
    genSrcFileList.doFirst(task -> {
      outFile.delete();
      outFile.getParentFile().mkdirs();
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
      for (File dir : getMainJavaSrcDirs(pro)) {
        Files.walk(Paths.get(dir.toURI()))
          .filter(Files::isRegularFile)
          .forEach(f -> {
            try {
              writer.write(f.toAbsolutePath() + "\n");
            } catch (IOException e) {
              throw new TaskExecutionException(task, e);
            }
          });
      }
      } catch (IOException e) {
        throw new TaskExecutionException(task, e);
      }
    });

    final Exec i18nXgettext = pro.getTasks().create("generatePot", Exec.class);
    i18nXgettext.setGroup("JOSM-i18n");
    i18nXgettext.setDescription("Extracts translatable strings from the source code");
    i18nXgettext.dependsOn(genSrcFileList);

    final String propOutBaseName = "outBaseName";
    i18nXgettext.getExtensions().getExtraProperties().set(propOutBaseName, null); // Value is set after project is evaluated
    final File outDir = new File(pro.getBuildDir(), "po");
    i18nXgettext.getInputs().files(getMainJavaSrcDirs(pro));
    i18nXgettext.getOutputs().dir(outDir);
    i18nXgettext.setWorkingDir(pro.getProjectDir());
    i18nXgettext.setExecutable("xgettext");
    i18nXgettext.args(
      "--from-code=UTF-8", "--language=Java",
      "--files-from=" + pro.getBuildDir() + "/srcFileList.txt",
      "--output-dir=" + outDir.getAbsolutePath(),
      "--add-comments=i18n:",
      "-k", "-ktrc:1c,2", "-kmarktrc:1c,2", "-ktr", "-kmarktr", "-ktrn:1,2", "-ktrnc:1c,2,3"
    );
    pro.afterEvaluate(p -> {
      i18nXgettext.setProperty(propOutBaseName, "josm-plugin_" + p.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName());
      i18nXgettext.args(
        "--default-domain=" + i18nXgettext.property(propOutBaseName),
        "--package-name=josm-plugin/" + i18nXgettext.property(propOutBaseName),
        "--package-version=" + p.getVersion()
      );
      if (JosmPluginExtension.forProject(p).getI18n().getBugReportEmail() != null) {
        i18nXgettext.args("--msgid-bugs-address=" + JosmPluginExtension.forProject(p).getI18n().getBugReportEmail());
      }
      if (JosmPluginExtension.forProject(p).getI18n().getCopyrightHolder() != null) {
        i18nXgettext.args("--copyright-holder=" + JosmPluginExtension.forProject(p).getI18n().getCopyrightHolder());
      }
    });
    i18nXgettext.doFirst(task -> {
      outDir.mkdirs();
      task.getLogger().lifecycle(i18nXgettext.getExecutable());
      for (final Object arg : i18nXgettext.getArgs()) {
        task.getLogger().lifecycle("  " + arg);
      }
    });

    i18nXgettext.doLast(task -> {
      final File destFile = new File(outDir, task.property(propOutBaseName) + ".pot");
      final Map<String,String> replacements = new HashMap<>();
      replacements.put("(C) YEAR", "(C) " + Year.now().getValue());
      replacements.put("charset=CHARSET", "charset=UTF-8");
      try {
      moveFileAndReplaceStrings(
        new File(outDir, task.property(propOutBaseName) + ".po"),
        destFile,
        line -> (
          line.startsWith("#: ")
            ? line.substring(0, 3) + JosmPluginExtension.forProject(pro).getI18n().getPathTransformer().invoke(line.substring(3))
            : line
        ),
        replacements,
        "\n#. Plugin description for " + pro.getName() + "\nmsgid \"" + JosmPluginExtension.forProject(pro).getManifest().getDescription() + "\"\nmsgstr \"\"\n"
      );
      } catch (IOException e) {
        throw new TaskExecutionException(task, e);
      }
    });
  }

  private void moveFileAndReplaceStrings(final File src, final File dest, final Function<String, String> lineTransform, final Map<String,String> replacements, final String appendix) throws IOException {
    try (
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.UTF_8));
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
    ) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = lineTransform.apply(line);
        final String[] keys = replacements.keySet().toArray(new String[replacements.size()]);
        for (final String key : keys) {
          if (line.contains(key) && replacements.containsKey(key)) {
            line = line.replace(key, replacements.get(key));
            replacements.remove(key);
          }
        }
        writer.write(line + "\n");
      }
      if (appendix != null) {
        writer.write(appendix);
      }
    }
    src.delete();
  }
}
