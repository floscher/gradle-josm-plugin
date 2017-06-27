package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project

/**
 *
 */
class JosmPluginExtension {
  protected static def Project project = null;

  /**
   * If this is set to {@code true}, the *.jar file built for the project is put into the
   * `$JOSM_HOME/plugins/` directory for the tasks `runJosm`/`debugJosm`
   */
  def boolean isPlugin = true
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
   * <strong>Default value:</strong>
   * <ul>
   * <li>https://josm.openstreetmap.de/download/</li>
   * <li>https://svn.openstreetmap.org/applications/editors/josm/dist/</li>
   * <li>https://josm.openstreetmap.de/nexus/content/repositories/releases/</li>
   * <li>https://josm.openstreetmap.de/nexus/content/repositories/snapshots/</li>
   * </ul>
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
}
