package org.openstreetmap.josm.gradle.plugin.config

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.tasks.util.PatternFilterable
import org.openstreetmap.josm.gradle.plugin.josm
import org.openstreetmap.josm.gradle.plugin.useSeparateTmpJosmDirs
import java.io.File
import java.net.URI

/**
 * This extension is added to the project as `project.josm`
 *
 * @constructor instantiates the extension, takes project properties into account
 */
open class JosmPluginExtension(private val project: Project) {
  /**
   * The version number of JOSM against which the plugin should be compiled.
   *
   * **Default value:** The value of the property `plugin.compile.version` or `null` if that property is not set.
   */
  var josmCompileVersion: String? = project.findProperty("plugin.compile.version")?.toString()

  /**
   * This is required, when you want to run the task `debugJosm`. Set this to the port
   * on which you want to listen for the debug output.
   */
  var debugPort: Int? = null

  /**
   * Iff this is `true`, then after the build has finished, all skipped tasks are listed like this:
   * ```
   * Skipped tasks:
   * ⏭️  :taskA (UP-TO-DATE)
   * ⏭️  :taskB (NO-SOURCE)
   * ⏭️  :taskC (FROM-CACHE)
   * ```
   * The message is printed via [Project.getLogger], logging level is [org.gradle.api.logging.Logger.lifecycle].
   *
   * **Default value:** `true`
   * @since v0.5.1
   */
  var logSkippedTasks: Boolean = true

  /**
   * Iff this is `true`, the coverage numbers for instruction, branch and line coverage are logged for every tasks
   * with type [org.gradle.testing.jacoco.tasks.JacocoReport].
   *
   * The format is as follows:
   * ```
   * Instruction coverage: 25.0000 % (25 of 100)
   *      Branch coverage: 50.0000 % (5 of 10)
   *        Line coverage: 75.0000 % (3 of 4)
   * ```
   * The message is printed via [Project.getLogger], logging level is [org.gradle.api.logging.Logger.lifecycle].
   *
   * **Default value:** `true`
   * @since v0.5.1
   */
  var logJacocoCoverage: Boolean = true

  /**
   * Iff this is `true`, the number of seconds each task needed for execution is logged,
   * when execution of a task finishes.
   *
   * The line appended to the logging output of each task looks as follows:
   * ```
   *   🏁 Finished after 1.23 seconds.
   * ```
   * The message is printed via [Project.getLogger], logging level is [org.gradle.api.logging.Logger.lifecycle].
   *
   * **Default value:** `true`
   * @since v0.5.1
   */
  var logTaskDuration: Boolean = true

  /**
   * The directory in which the JOSM preferences for the JOSM instance used by `runJosm` and `debugJosm` are stored.
   *
   * **Default value:** `$buildDir/.josm/pref`
   * @since v0.4.0
   */
  var tmpJosmPrefDir = File(project.buildDir, ".josm/pref")

  /**
   * The directory in which the JOSM cache for the JOSM instance used by `runJosm` and `debugJosm` is stored.
   *
   * **Default value:** `$buildDir/.josm/cache` (for JOSM versions < 7841, `${josm.tmpJosmPrefDir}/cache` is used)
   * @since v0.4.0
   */
  var tmpJosmCacheDir = File(project.buildDir, ".josm/cache")

  /**
   * The directory in which the JOSM user data for the JOSM instance used by `runJosm` and `debugJosm` is stored.
   *
   * **Default value:** `$buildDir/.josm/userdata` (for JOSM versions < 7841,  `${josm.tmpJosmPrefDir}` is used)
   * @since v0.4.0
   */
  var tmpJosmUserdataDir = File(project.buildDir, ".josm/userdata")

  init {
    project.afterEvaluate {
      if (!it.useSeparateTmpJosmDirs()) {
        it.logger.warn("You are using a very old version of JOSM (< 7841) that doesn't have separate directories for cache and user data.")
        project.extensions.josm.tmpJosmCacheDir = File(project.extensions.josm.tmpJosmPrefDir, "cache")
        project.extensions.josm.tmpJosmUserdataDir = project.extensions.josm.tmpJosmPrefDir
        it.logger.warn("These settings are now overwritten as follows: tmpJosmCacheDir=${tmpJosmCacheDir.absolutePath} tmpJosmUserdataDir=${tmpJosmUserdataDir.absolutePath}")
      }
    }
  }

  /**
   * The directory where the default `preferences.xml` file is located, which will be used
   * when the directory defined in `tmpJosmHome` is empty.
   *
   * **Default value:** `$projectDir/config/josm`
   */
  var josmConfigDir: File = File("${project.projectDir}/config/josm")

  /**
   * When determining on which JOSM plugins this project depends, dependency chains are followed this number of steps.
   * This number is the termination criterion when recursively searching for JOSM plugins that are required through [JosmManifest.pluginDependencies].
   */
  var maxPluginDependencyDepth: Int = 10
    set(value) {
      require(value >= 0) {
        "For property `maxPluginDependencyDepth` only nonnegative integer values are allowed! You are trying to set it to $value."
      }
    }

  /**
   * When packing the dependencies of the `packIntoJar` configuration into the distribution *.jar,
   * this closure is applied to the file tree. If you want to exclude certain files in your dependencies into
   * your release, you can modify this.
   * By default the `/META-INF/` directory of dependencies is discarded.
   *
   * **Default value:** `{ it.exclude("META-INF/&#42;&#42;/&#42;") }`
   * @see org.gradle.api.file.FileTree.matching(groovy.lang.Closure)
   * @see PatternFilterable
   */
  var packIntoJarFileFilter: (PatternFilterable) -> PatternFilterable = { it.exclude("META-INF/**/*") }

  /**
   * Set the [packIntoJarFileFilter] with a Groovy [Closure]
   */
  public fun packIntoJarFileFilter(closure: Closure<PatternFilterable>) {
    packIntoJarFileFilter = { closure.call(it) }
  }

  /**
   * The repositories that are added to the repository list.
   *
   * **Default value (in this order):**
   * 1. Nexus repo for JOSM releases: [https://josm.openstreetmap.de/nexus/content/repositories/releases/](https://josm.openstreetmap.de/nexus/content/repositories/releases/) (as Maven repo)
   * 2. Download page for JOSM releases and snapshots: [https://josm.openstreetmap.de/download/](https://josm.openstreetmap.de/download/) (as custom Ivy repo, the `Archiv` subdirectory is also included)
   * 3. Nexus repo for JOSM snapshots: [https://josm.openstreetmap.de/nexus/content/repositories/snapshots/](https://josm.openstreetmap.de/nexus/content/repositories/snapshots/) (as Maven repo)
   * 4. Directory in SVN repo where JOSM plugins are published: [https://svn.openstreetmap.org/applications/editors/josm/dist/](https://svn.openstreetmap.org/applications/editors/josm/dist/) (as custom Ivy repo)
   *
   * @see RepositoryHandler
   */
  var repositories: (RepositoryHandler) -> Unit = fun (rh: RepositoryHandler) {
    rh.maven {
      it.url = URI("https://josm.openstreetmap.de/nexus/content/repositories/releases/")
    }
    rh.ivy {
      it.url = URI("https://josm.openstreetmap.de/download/")
      it.layout("pattern", Action<IvyPatternRepositoryLayout> {
        it.artifact("[artifact].jar")
        it.artifact("[artifact]-[revision].jar")
        it.artifact("[artifact]-snapshot-[revision].jar")
        it.artifact("Archiv/[artifact]-snapshot-[revision].jar")
      })
    }
    rh.maven {
      it.url = URI("https://josm.openstreetmap.de/nexus/content/repositories/snapshots/")
    }
    rh.ivy {
      it.url = URI("https://svn.openstreetmap.org/applications/editors/josm/dist/")
      it.layout("pattern", Action<IvyPatternRepositoryLayout> {
        it.artifact("[artifact].jar")
      })
    }
  }

  /**
   * Set the [repositories] with a Groovy [Closure] (replaces any previous setting).
   */
  public fun repositories(closure: Closure<RepositoryHandler>) {
    repositories = { closure.call(it) }
  }

  /**
   * Configuration options for i18n
   */
  val i18n: I18nConfig = I18nConfig(project)

  /**
   * Configure the field [JosmPluginExtension.i18n] using a Groovy [Closure]
   */
  public fun i18n(c: Closure<I18nConfig>) {
    project.configure(i18n, c)
  }

  /**
   * Configure the field [JosmPluginExtension.i18n] using an [Action].
   */
  public fun i18n(a: Action<I18nConfig>) {
    a.execute(i18n)
  }

  /**
   * The manifest for the JOSM plugin
   */
  val manifest: JosmManifest = JosmManifest(project)

  /**
   * Configure the field [manifest] using a Groovy [Closure].
   */
  public fun manifest(c: Closure<JosmManifest>) {
    project.configure(manifest, c)
  }

  /**
   * Configure the field [manifest] using an [Action].
   */
  public fun manifest(a: Action<JosmManifest>) {
    a.execute(manifest)
  }

  /**
   * Determines if the version number of the project should be derived from the
   * version control system (git, or SVN if git is not present).
   *
   * In case neither git nor SVN is present, the project version will not be modified regardless of this setting.
   *
   * **Default value:** `true`
   * @since 0.5.2
   */
  var versionFromVcs = true

  /**
   * Determines if the leading 'v' character is trimmed from the beginning of [Project.getVersion]
   *
   * **Default value:** `false` (will be changed to `true` as soon as `0.6.0` is released)
   * @since 0.5.2
   */
  var versionWithoutLeadingV = false // TODO: Switch to true with the release of 0.6.0

  @Deprecated(message = "Will soon be removed with release 0.6.0") // TODO: Remove with the release of 0.6.0
  companion object {
    /**
     * Returns the [JosmPluginExtension] for a particular [Project].
     */
    @Deprecated("Will soon be removed with release 0.6.0", ReplaceWith("project.extensions.josm"))
    @JvmStatic
    fun forProject(project: Project): JosmPluginExtension {
      return project.getExtensions().getByType(JosmPluginExtension::class.java)
    }
  }
}
