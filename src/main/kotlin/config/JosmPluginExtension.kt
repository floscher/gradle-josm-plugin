package org.openstreetmap.josm.gradle.plugin.config

import groovy.lang.Closure
import java.io.File
import java.net.URI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.artifacts.repositories.RepositoryLayout
import org.gradle.api.tasks.util.PatternFilterable

class JosmPluginExtension(project: Project) {
  private val project: Project = project
  /**
   * The version number of JOSM against which the plugin should be compiled.
   *
   * <p><strong>Default value:</strong> The value of the property <code>plugin.compile.version</code> or <code>null</code> if that property is not set.</p>
   */
  var josmCompileVersion: String? = project.findProperty("plugin.compile.version")?.toString()

  /**
   * This is required, when you want to run the task {@code debugJosm}. Set this to the port
   * on which you want to listen for the debug output.
   */
  var debugPort: Int? = null

  /**
   * The directory which should be used as JOSM home directory when executing the {@code runJosm} or
   * {@code debugJosm} tasks.
   *
   * <p><strong>Default value:</strong> <code>$buildDir/.josm</code></p>
   */
  var tmpJosmHome: File = File("${project.buildDir}/.josm")

  /**
   * The directory where the default {@code preferences.xml} file is located, which will be used
   * when the directory defined in {@code tmpJosmHome} is empty.
   *
   * <p><strong>Default value:</strong> <code>$projectDir/config/josm</code></p>
   */
  var josmConfigDir: File = File("${project.projectDir}/config/josm")

  /**
   * When determining on which JOSM plugins this project depends, dependency chains are followed this number of steps.
   * This number is the termination criterion when recursively searching for JOSM plugins that are required through [JosmManifest.pluginDependencies].
   */
  var maxPluginDependencyDepth: Int = 10
    set(value) {
      maxPluginDependencyDepth = Math.max(0, value)
    }

  /**
   * When packing the dependencies of the {@code packIntoJar} configuration into the distribution *.jar,
   * this closure is applied to the file tree. If you want to exclude certain files in your dependencies into
   * your release, you can modify this.
   * By default the {@code META-INF} directory of dependencies is discarded.
   * <p><strong>Default value:</strong> <code>{ it.exclude("META-INF/&#42;&#42;/&#42;") }</code></p>
   * @see org.gradle.api.file.FileTree#matching(groovy.lang.Closure)
   * @see PatternFilterable the closure takes an instance of PatternFilterable as argument
   */
  var packIntoJarFileFilter: (PatternFilterable) -> PatternFilterable = { pf: PatternFilterable -> pf.exclude("META-INF/**/*") }

  /**
   * Set the [packIntoJarFileFilter] with a Groovy [Closure]
   */
  public fun packIntoJarFileFilter(closure: Closure<PatternFilterable>) {
    packIntoJarFileFilter = { closure.call(it) }
  }

  /**
   * The repositories that are added to the repository list.
   * <p><strong>Default value (in this order):</strong>
   * <ol>
   * <li><a href="https://josm.openstreetmap.de/nexus/content/repositories/releases/">https://josm.openstreetmap.de/nexus/content/repositories/releases/</a></li>
   * <li><a href="https://josm.openstreetmap.de/download/">https://josm.openstreetmap.de/download/</a></li>
   * <li><a href="https://svn.openstreetmap.org/applications/editors/josm/dist/">https://svn.openstreetmap.org/applications/editors/josm/dist/</a></li>
   * <li><a href="https://josm.openstreetmap.de/nexus/content/repositories/snapshots/">https://josm.openstreetmap.de/nexus/content/repositories/snapshots/</a></li>
   * </ol></p>
   * For details see <a href="https://github.com/floscher/gradle-josm-plugin/tree/master/src/main/groovy/org/openstreetmap/josm/gradle/plugin/JosmPluginExtension.groovy">the source file</a>.
   * @see RepositoryHandler the closure takes an instance of RepositoryHandler as argument
   */
  var repositories = fun (rh: RepositoryHandler) {
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
    rh.ivy {
      it.url = URI("https://svn.openstreetmap.org/applications/editors/josm/dist/")
      it.layout("pattern", Action<IvyPatternRepositoryLayout> {
        it.artifact("[artifact].jar")
      })
    }
    rh.maven {
      it.url = URI("https://josm.openstreetmap.de/nexus/content/repositories/snapshots/")
    }
  }

  /**
   * Set the [repositories] with a Groovy [Closure] (replaces any previous setting).
   */
  public fun repositories(closure: Closure<RepositoryHandler>) {
    repositories = { closure.call(it) }
  }

  /**
   * Configuration options for i18n (read-only)
   * @see #i18n(Closure)
   */
  val i18n: I18nConfig = I18nConfig(project)

  /**
   * Set the field [i18n] using a Groovy [Closure]
   */
  public fun i18n(c: Closure<I18nConfig>) {
    project.configure(i18n, c)
  }

  /**
   * The manifest for the JOSM plugin (read-only)
   * @see #manifest(Closure)
   */
  val manifest: JosmManifest = JosmManifest(project)

  /**
   * Supply a {@link Closure} to this method, which configures the field {@link #manifest}.
   */
  public fun manifest(c: Closure<JosmManifest>) {
    project.configure(manifest, c)
  }

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
