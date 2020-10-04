package org.openstreetmap.josm.gradle.plugin.config

import groovy.lang.Closure
import org.gradle.api.Project
import org.openstreetmap.josm.gradle.plugin.GitDescriber
import java.io.File
import java.io.Serializable
import java.util.regex.Pattern

/**
 * Holds configuration options regarding internationalization.
 */
class I18nConfig: Serializable {

  companion object {
    private val LINE_NUMBER_PATTERN = Pattern.compile(".*:([1-9][0-9]*)")
  }

  /**
   * E-Mail address to which bugs regarding i18n should be reported.
   * This will be put into the *.pot files that are forwarded to the translators.
   *
   * **Default value:** `null`
   * @since 0.2.0
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
   * @since 0.2.0
   */
  var copyrightHolder: String? = null

  /**
   * The main language is in this context the language used in the raw strings
   * occuring in the source code.
   * It's the starting point on which the translators will base their translations.
   * Use the language codes, which you also use to name the *.po, *.mo or *.lang files.
   *
   * **Default value:** `en`
   * @since 0.3.1
   */
  var mainLanguage: String = "en"

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
   * @see getPathTransformer
   */
  var pathTransformer: (String) -> String = {a -> a}

  /**
   * Alternative to the setter of property [pathTransformer] using a Groovy [Closure].
   */
  fun pathTransformer(closure: Closure<String>) {
    pathTransformer = { closure.call(it) }
  }

  @Deprecated("This method now expects the project root directory (`project.projectDir`) as first parameter!", replaceWith = ReplaceWith("getPathTransformer(project.projectDir, repoUrl)"))
  fun getPathTransformer(repoUrl: String): (String) -> String = throw IllegalAccessException("The API changed! Add the project root directory as first method parameter!")

  /**
   * Creates a path transformer that replaces an absolute file path of the *.pot file with a URL
   * to a hosted instance of the project.
   * Supply a base URL to a source code browser on the web, it will be transformed to the full URL as follows:
   *
   * `$repoUrl/$gitCommitHash/$filePathRelativeToProjectRoot#L$lineNumber`
   *
   * Good values would be e.g. `gitlab.com/myself/MyAwesomeProject/-/blob` or `github.com/myself/MyAwesomeProject/blob`
   *
   * @param projectDir the project directory that will be replaced, also the git hash is calculated for that directory
   * @param repoUrl the supplied base URL
   * @since 0.4.7 (parameter [projectDir] added in 0.7.1)
   */
  fun getPathTransformer(projectDir: File, repoUrl: String) = { path: String ->
    val sourceFileMatcher = LINE_NUMBER_PATTERN.matcher(path)
    val (sourceFilePath, lineNumber) = if (sourceFileMatcher.matches()) {
      val lineNumber = sourceFileMatcher.group(1)
      path.substring(0, path.length - lineNumber.length - 1) to lineNumber
    } else {
      path to null
    }
    if (sourceFilePath.startsWith(projectDir.absolutePath)) {
      val relativePath = sourceFilePath.substring(projectDir.absolutePath.length).trim('/')
      "$repoUrl/${GitDescriber(projectDir).commitHash()}/$relativePath" + if (lineNumber == null) { "" } else { "#L$lineNumber"}
    } else {
      path
    }
  }
}
