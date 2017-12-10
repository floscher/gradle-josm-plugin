package org.openstreetmap.josm.gradle.plugin.config

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternFilterable
import org.openstreetmap.josm.gradle.plugin.JosmPlugin

/**
 * Contains the available configuration options for the Gradle plugin.
 */
@groovy.transform.CompileStatic
class JosmPluginExtension {
  private final Project project = JosmPlugin.currentProject;
  /**
   * The version number of JOSM against which the plugin should be compiled.
   *
   * <p><strong>Default value:</strong> The value of the property <code>plugin.compile.version</code> or <code>null</code> if that property is not set.</p>
   */
  def String josmCompileVersion = project.findProperty("plugin.compile.version")
  /**
   * This is required, when you want to run the task {@code debugJosm}. Set this to the port
   * on which you want to listen for the debug output.
   */
  def Integer debugPort = null
  /**
   * The directory which should be used as JOSM home directory when executing the {@code runJosm} or
   * {@code debugJosm} tasks.
   *
   * <p><strong>Default value:</strong> <code>$buildDir/.josm</code></p>
   */
  def File tmpJosmHome = new File("${project.buildDir}/.josm")
  /**
   * The directory where the default {@code preferences.xml} file is located, which will be used
   * when the directory defined in {@code tmpJosmHome} is empty.
   *
   * <p><strong>Default value:</strong> <code>$projectDir/config/josm</code></p>
   */
  def File josmConfigDir = new File("${project.projectDir}/config/josm")
  /**
   * When determining on which JOSM plugins this project depends, dependency chains are followed this number of steps.
   * This number is the termination criterion when recursively searching for JOSM plugins that are required through {@link JosmManifest#pluginDependencies}.
   */
  def int maxPluginDependencyDepth = 10
  /**
   * When packing the dependencies of the {@code packIntoJar} configuration into the distribution *.jar,
   * this closure is applied to the file tree. If you want to exclude certain files in your dependencies into
   * your release, you can modify this.
   * By default the {@code META-INF} directory of dependencies is discarded.
   * <p><strong>Default value:</strong> {@code { pf.exclude("META-INF/&#42;&#42;/*") }}</p>
   * @see org.gradle.api.file.FileTree#matching(groovy.lang.Closure)
   * @see PatternFilterable the closure takes an instance of PatternFilterable as argument
   */ // Note: The character sequence slash-asterisk-asterisk trips up groovydoc. That's why the string in the next line is concatenated.
  def Closure packIntoJarFileFilter = {PatternFilterable pf -> pf.exclude('META-INF/' + '**/*') }
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
  def Closure repositories = { RepositoryHandler rh ->
    rh.maven { MavenArtifactRepository mar ->
      mar.url = new URI('https://josm.openstreetmap.de/nexus/content/repositories/releases/')
    }
    rh.ivy { IvyArtifactRepository iar ->
      iar.url = new URI('https://josm.openstreetmap.de/download/')
      iar.layout 'pattern', { IvyPatternRepositoryLayout rl ->
        rl.artifact "[artifact].jar"
        rl.artifact "[artifact]-[revision].jar"
        rl.artifact "[artifact]-snapshot-[revision].jar"
        rl.artifact "Archiv/[artifact]-snapshot-[revision].jar"
      }
    }
    rh.ivy { IvyArtifactRepository iar ->
      iar.url = new URI("https://svn.openstreetmap.org/applications/editors/josm/dist/")
      iar.layout "pattern", { IvyPatternRepositoryLayout rl ->
        rl.artifact "[artifact].jar"
      }
    }
    rh.maven { MavenArtifactRepository mar ->
      mar.url = new URI('https://josm.openstreetmap.de/nexus/content/repositories/snapshots/')
    }
  }

  /**
   * Configuration options for i18n (read-only)
   * @see #i18n(Closure)
   */
  final def I18nConfig i18n = new I18nConfig();

  /**
   * Supply a {@link Closure} to this method, which configures the field {@link #i18n}.
   */
  public void i18n(final Closure c) {
    project.configure(i18n, c)
  }

  /**
   * The manifest for the JOSM plugin (read-only)
   * @see #manifest(Closure)
   */
  final def JosmManifest manifest = new JosmManifest()

  /**
   * Supply a {@link Closure} to this method, which configures the field {@link #manifest}.
   */
  public void manifest(final Closure c) {
    project.configure(manifest, c)
  }
}
