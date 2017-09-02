package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project

/**
 * Contains the available configuration options for the Gradle plugin.
 */
class JosmPluginExtension {
  protected static def Project project = null;

  /**
   * If this is set to {@code true}, the *.jar file built for the project is put into the
   * {@code $JOSM_HOME/plugins/} directory for the tasks {@code runJosm} or {@code debugJosm}
   */
  def boolean isPlugin = true
  /**
   * The version number of JOSM against which the plugin should be compiled.
   *
   * <p><strong>Default:</strong> The value of the property <code>plugin.compile.version</code> or <code>null</code> if that property is not set.</p>
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
   * This property defines, how the resulting *.jar file should be called in the <code>$JOSM_HOME/plugins/</code>
   * directory, e.g. <code>MyAwesomePlugin.jar</code>. If this is not set (or set to null), the default name determined by
   * Gradle would be used (<code>${baseName}-${appendix}-${version}-${classifier}.jar</code>).
   */
  def String jarName
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
  def Manifest manifest = new Manifest(project)
}
