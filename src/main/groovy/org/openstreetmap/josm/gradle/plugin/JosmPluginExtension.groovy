package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project

/**
 * Contains the available configuration options for the Gradle plugin.
 */
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
   * The repositories that are added to the repository list.
   * <p><strong>Default value (in this order):</strong>
   * <ol>
   * <li><a href="https://josm.openstreetmap.de/download/">https://josm.openstreetmap.de/download/</a></li>
   * <li><a href="https://svn.openstreetmap.org/applications/editors/josm/dist/">https://svn.openstreetmap.org/applications/editors/josm/dist/</a></li>
   * <li><a href="https://josm.openstreetmap.de/nexus/content/repositories/releases/">https://josm.openstreetmap.de/nexus/content/repositories/releases/</a></li>
   * <li><a href="https://josm.openstreetmap.de/nexus/content/repositories/snapshots/">https://josm.openstreetmap.de/nexus/content/repositories/snapshots/</a></li>
   * </ol></p>
   * For details see <a href="https://github.com/floscher/gradle-josm-plugin/tree/master/src/main/groovy/org/openstreetmap/josm/gradle/plugin/JosmPluginExtension.groovy">the source file</a>.
   */
  def Closure repositories = {
    ivy {
      url 'https://josm.openstreetmap.de/download/'
      layout 'pattern', {
        artifact "[artifact].jar"
        artifact "[artifact]-[revision].jar"
        artifact "[artifact]-snapshot-[revision].jar"
        artifact "Archiv/[artifact]-snapshot-[revision].jar"
      }
    }
    ivy {
      url "https://svn.openstreetmap.org/applications/editors/josm/dist/"
      layout "pattern", {
        artifact "[artifact].jar"
      }
    }
    maven {
      url 'https://josm.openstreetmap.de/nexus/content/repositories/releases/'
    }
    maven {
      url 'https://josm.openstreetmap.de/nexus/content/repositories/snapshots/'
    }
  }

  /**
   * The manifest for the JOSM plugin. This is initialized as soon as the project has been evaluated.
   */
  final def JosmManifest manifest = new JosmManifest()

  public void manifest(final Closure c) {
    project.configure(manifest, c)
  }
}
