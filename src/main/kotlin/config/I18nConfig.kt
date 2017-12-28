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
   *
   * **Default value:** `null`
   * @since v0.2.0
   */
  var bugReportEmail: String? = null

  /**
   * Person or organization that holds the copyright on the project.
   * This will appear in the header of the *.pot file as follows:
   * ```
   * # Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER
   * ```
   *
   * **Default value:** `null`
   * @since v0.2.0
   */
  var copyrightHolder: String? = null

  /**
   * Replaces each occurence of the value of [Project.getProjectDir()] in all
   * file paths of the generated *.pot file.
   *
   * For each translated string the *.pot file points to the location in the
   * source code (lines starting with `#: `).
   * Each of those lines is put through this transformer (without the leading
   * `#: `).
   *
   * **Default value:** `{a -> a}` (identity)
   * @see getGithubPathTransformer
   */
  var pathTransformer: (String) -> String = {a -> a};

  /**
   * Alternative to [setPathTransformer()], can set the [pathTransformer]
   * field using a Groovy [Closure].
   */
  fun pathTransformer(closure: Closure<String>) {
    pathTransformer = { closure.call(it) }
  }

  /**
   * Creates a path transformer for a project hosted on GitHub.
   * Supply a repo slug (`username/repo`) and this method will return a function,
   * which you can use as value for the field [pathTransformer].
   */
  fun getGithubPathTransformer(repoSlug: String): (String) -> String {
    return fun(path: String): String {
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
            return "github.com/$repoSlug/blob/${gitProcess.inputStream.bufferedReader().readText().trim()}" +
              filePath.substring(projectPath.length) +
              (if (lineNumber == null ) "" else "#L" + lineNumber + ':' + lineNumber)
          }
          return path
        }
        throw GradleException("Failed to determine current commit hash!\n" + gitProcess.errorStream.bufferedReader().readText())
    };
  }
}
