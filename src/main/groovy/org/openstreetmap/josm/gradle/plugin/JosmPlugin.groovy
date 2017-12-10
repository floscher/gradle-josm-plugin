package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.setup.BasicTaskSetup
import org.openstreetmap.josm.gradle.plugin.setup.I18nTaskSetup
import org.openstreetmap.josm.gradle.plugin.setup.MinJosmVersionSetup
import org.openstreetmap.josm.gradle.plugin.setup.PluginTaskSetup

import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.jar.Manifest

/**
 * Main class of the plugin, sets up the {@code requiredPlugin} configuration,
 * the additional repositories and the custom tasks.
 */
class JosmPlugin implements Plugin<Project> {
  private static Project currentProject;

  public static Project getCurrentProject() {
    if (currentProject == null) {
      throw new IllegalStateException("Currently the gradle-josm-plugin is not applied to a Gradle project, but you want to access the project to which the gradle-josm-plugin is applied. This should not happen ;).")
    }
    return currentProject
  }

  /**
   * Set up the JOSM plugin.
   * Creates the tasks this plugin provides, defines the {@code josm} extension, adds the repositories where JOSM specific dependencies can be found.
   * @see {@link Plugin#apply(T)}
   */
  synchronized void apply(final Project project) {
    JosmPlugin.currentProject = project;

    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.plugins.findPlugin(JavaPlugin) == null) {
      project.apply plugin: 'java'
    }
    // Define 'josm' extension
    project.extensions.create("josm", JosmPluginExtension)

    project.configurations {
      // Configuration for JOSM plugins that are required for this plugin. Normally there's no need to set these manually, these are set based on the manifest configuration
      implementation.extendsFrom(requiredPlugin)
      // Configuration for libraries on which the project depends and which should be packed into the built *.jar file.
      implementation.extendsFrom(packIntoJar)
    }

    project.repositories(project.josm.repositories)

    project.tasks.jar.doFirst {
      it.project.jar {
        manifest.attributes it.project.josm.manifest.createJosmPluginJarManifest(it.project)
        from project.configurations.packIntoJar.collect{
          it.isDirectory()
          ? project.fileTree(it).matching(project.josm.packIntoJarFileFilter)
          : project.zipTree(it).matching(project.josm.packIntoJarFileFilter)
        }
      }
    }

    project.afterEvaluate {
      // Adding dependencies for JOSM and the required plugins
      project.dependencies.add('implementation', 'org.openstreetmap.josm:josm:'+project.josm.josmCompileVersion)
      requirePlugins(project, project.josm.manifest.pluginDependencies.toArray(new String[project.josm.manifest.pluginDependencies.size()]))
    }

    new BasicTaskSetup().setup()
    new I18nTaskSetup().setup()
    new PluginTaskSetup().setup()
    new MinJosmVersionSetup().setup()
    JosmPlugin.currentProject = null;
  }

  /**
   * Convenience method
   */
  private void requirePlugins(Project pro, String... pluginNames) {
    requirePlugins(0, pro, pluginNames)
  }
  /**
   * Recursively add the required plugins and any plugins that these in turn require to the configuration `requiredPlugin`.
   * @param recursionDepth starts at 0, each time this method is called from within itself, this is incremented by one
   * @param pro the project, which requires the JOSM plugins given with the last parameter
   * @param pluginNames the names of all required JOSM plugins. Transitive dependencies can be omitted, these will be filled in by this method.
   * @throws GradleException if the parameter {@code recursionDepth} reaches the current limit of 10
   */
  private void requirePlugins(final int recursionDepth, final Project pro, final String... pluginNames) {
    if (recursionDepth >= pro.josm.maxPluginDependencyDepth) {
      throw new GradleException(sprintf("Dependency tree of required JOSM plugins is too deep (>= %d steps). Aborting resolution of required JOSM plugins.", pro.josm.maxPluginDependencyDepth))
    }
    pluginNames.each({ pluginName ->
      pluginName = pluginName.trim()
      pro.logger.info "  " * recursionDepth + "Add required JOSM plugin '{}' to classpath…", pluginName

      final def tmpConf = pro.configurations.create('tmpConf'+recursionDepth)
      final def pluginDep = pro.dependencies.add('tmpConf'+recursionDepth, 'org.openstreetmap.josm.plugins:' + pluginName + ':', {changing = true})
      if (pro.configurations.requiredPlugin.dependencies.contains(pluginDep)) {
        pro.logger.info "JOSM plugin '{}' is already on the classpath.", pluginName
      } else {
        tmpConf.fileCollection(pluginDep).files.each { jarFile ->
          def zipFile = new ZipFile(jarFile)
          def zipEntries = zipFile.entries()
          while (zipEntries.hasMoreElements()) {
            def zipEntry = zipEntries.nextElement()
            if ('META-INF/MANIFEST.MF'.equals(zipEntry.name)) {
              def requirements = new Manifest(zipFile.getInputStream(zipEntry)).mainAttributes.getValue("Plugin-Requires")
              if (requirements != null) {
                // If the plugin itself requires more plugins, recursively add them too.
                requirePlugins(Math.max(1, recursionDepth + 1), pro, requirements.split(';'))
              }
            }
          }
        }
        pro.configurations.requiredPlugin.dependencies.add(pluginDep)
      }
      pro.configurations.remove(tmpConf)
    })
  }
}
