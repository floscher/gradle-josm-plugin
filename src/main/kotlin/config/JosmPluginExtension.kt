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
   * Set the field [JosmPluginExtension.i18n] using a Groovy [Closure]
   */
  public fun i18n(c: Closure<I18nConfig>) {
    project.configure(i18n, c)
  }

  /**
   * The manifest for the JOSM plugin
   */
  val manifest: JosmManifest = JosmManifest(project)

  /**
   * Set the field [manifest] using a Groovy [Closure].
   */
  public fun manifest(c: Closure<JosmManifest>) {
    project.configure(manifest, c)
  }

  /** @suppress */
  companion object {
    /**
     * Returns the [JosmPluginExtension] for a particular [Project].
     */
    @JvmStatic
    fun forProject(project: Project): JosmPluginExtension {
      return project.getExtensions().getByType(JosmPluginExtension::class.java)
    }
  }
}
