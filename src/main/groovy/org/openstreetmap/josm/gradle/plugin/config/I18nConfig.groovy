package org.openstreetmap.josm.gradle.plugin.config;

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.JosmPlugin

import java.util.regex.Matcher
import java.util.regex.Pattern

@groovy.transform.CompileStatic
public class I18nConfig {
  private final Project project = JosmPlugin.currentProject;
  /**
   * E-Mail address to which bugs regarding i18n should be reported.
   * This will be put into the *.pot files that are forwarded to the translators.
   * <p><strong>Default value:</strong> {@code null}</p>
   */
  def String bugReportEmail = null;
  /**
   * Person or organization that holds the copyright on the project.
   * This will appear in the header of the *.pot file as follows:
   * <pre># Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER</pre>
   */
  def String copyrightHolder = null;
  /**
   * Replaces the {@link Project#projectDir} in all file paths of the generated
   * *.pot file.
   *
   * For each translated string the *.pot file points to the location in the
   * source code (lines starting with <code>#: </code>).
   * Each of those lines is put through this transformer (without the leading
   * <code>#: </code>).
   * <p><strong>Default value:</strong> {@link Closure#IDENTITY}</p>
   * @see #getGithubPathTransformer(String) if your project is hosted on GitHub,
   *   you can retrieve a suitable pathTransformer via this method
   */
  def Closure pathTransformer = Closure.IDENTITY

  public final Closure getGithubPathTransformer(final String repoSlug) {
    return { final String path ->
      Matcher lineNumberMatcher = Pattern.compile(".*:([1-9][0-9]*)").matcher(path)
      String lineNumber = null
      String filePath = path
      if (lineNumberMatcher.matches()) {
        lineNumber = lineNumberMatcher.group(1)
        filePath = path.substring(0, path.length() - lineNumber.length() - 1)
      }
      def gitProcess = ['git', 'rev-parse', '--short', 'HEAD'].execute()
      gitProcess.waitFor()
      if (gitProcess.exitValue() == 0) {
        def projectPath = project.projectDir.absolutePath
        if (filePath.startsWith(projectPath)) {
          return "github.com/$repoSlug/blob/${gitProcess.in.text.trim()}" +
            filePath.substring(projectPath.length()) +
            (lineNumber == null ? '' : '#L' + lineNumber + ':' + lineNumber)
        }
        return path
      }
      throw new GradleException("Failed to determine current commit hash!\n" + gitProcess.err.text)
    }
  }
}
