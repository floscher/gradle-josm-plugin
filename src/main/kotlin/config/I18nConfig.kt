package org.openstreetmap.josm.gradle.plugin.config

import groovy.lang.Closure
import java.lang.Process
import java.lang.ProcessBuilder
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Holds configuration options regarding internationalization.
 */
class I18nConfig(project: Project) {
  private val project: Project = project
  /**
   * E-Mail address to which bugs regarding i18n should be reported.
   * This will be put into the *.pot files that are forwarded to the translators.
   * <p><strong>Default value:</strong> {@code null}</p>
   */
  var bugReportEmail: String? = null

  /**
   * Person or organization that holds the copyright on the project.
   * This will appear in the header of the *.pot file as follows:
   * <pre># Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER</pre>
   */
  var copyrightHolder: String? = null

  /**
   * Replaces the {@link Project#projectDir} in all file paths of the generated
   * *.pot file.
   *
   * For each translated string the *.pot file points to the location in the
   * source code (lines starting with <code>#: </code>).
   * Each of those lines is put through this transformer (without the leading
   * <code>#: </code>).
   * <p><strong>Default value:</strong> {@link Function#identity()}</p>
   * @see #getGithubPathTransformer(String) if your project is hosted on GitHub,
   *   you can retrieve a suitable pathTransformer via this method
   */
  var pathTransformer: (String) -> String = {a -> a};

  /**
   * Set the [pathTransformer] field using a Groovy [Closure].
   */
  fun pathTransformer(closure: Closure<String>) {
    pathTransformer = { string ->
      closure.call(string)
    }
  }

  fun getGithubPathTransformer(repoSlug: String): (String) -> String {
    return lambda@ { path ->
        val lineNumberMatcher: Matcher = Pattern.compile(".*:([1-9][0-9]*)").matcher(path)
        var lineNumber: String? = null
        var filePath: String = path
        if (lineNumberMatcher.matches()) {
          lineNumber = lineNumberMatcher.group(1)
          filePath = path.substring(0, path.length - lineNumber.length - 1)
        }
        val gitProcess: Process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
        gitProcess.waitFor()
        if (gitProcess.exitValue() == 0) {
          val projectPath: String = project.projectDir.absolutePath
          if (filePath.startsWith(projectPath)) {
            return@lambda "github.com/$repoSlug/blob/${gitProcess.inputStream.bufferedReader().readText().trim()}" +
              filePath.substring(projectPath.length) +
              (if (lineNumber == null ) "" else "#L" + lineNumber + ':' + lineNumber)
          }
          return@lambda path
        }
        throw GradleException("Failed to determine current commit hash!\n" + gitProcess.errorStream.bufferedReader().readText())
    };
  }
}
