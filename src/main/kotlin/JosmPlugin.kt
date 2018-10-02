package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.task.setupJosmTasks
import javax.inject.Inject

/**
 * Main class of the plugin, sets up the custom configurations <code>requiredPlugin</code> and <code>packIntoJar</code>,
 * the additional repositories and the custom tasks.
 */
class JosmPlugin @Inject constructor(val sourceDirectorySetFactory: SourceDirectorySetFactory?): Plugin<Project> {

  /**
   * Set up the JOSM plugin.
   *
   * Creates the tasks this plugin provides, defines the <code>josm</code> extension, adds the repositories where JOSM specific dependencies can be found.
   * Overrides <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/Plugin.html#apply-T-">Plugin.apply()</a>.
   */
  override fun apply(project: Project) {

    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.plugins.findPlugin(JavaPlugin::class.java) == null) {
      project.apply { it.plugin(JavaPlugin::class.java) }
    }

    // Define 'josm' extension
    project.extensions.create("josm", JosmPluginExtension::class.java, project)

    val jarTask = project.tasks.withType(Jar::class.java).getByName("jar")
    jarTask.doFirst { task ->
      jarTask.manifest.attributes(project.extensions.josm.manifest.createJosmPluginJarManifest())
      jarTask.from(
        task.project.configurations.getByName("packIntoJar").files.map { file ->
          if (file.isDirectory) {
            project.fileTree(file)
          } else {
            project.zipTree(file)
          }.matching {
            project.extensions.josm.packIntoJarFileFilter.invoke(it)
          }
        }
      )
    }

    project.afterEvaluate {
      if (project.extensions.josm.versionFromVcs && project.version == Project.DEFAULT_VERSION) {
        val version = try {
          GitDescriber(project.projectDir).describe(dirty = true)
        } catch (e: Exception) {
          project.logger.info("Error getting project version for ${project.projectDir} using git!", e)
          try {
            // Fall back to SVN revision if `git describe` does nor work
            SvnDescriber(project.projectDir).describe(dirty = true)
          } catch (e: Exception) {
            project.logger.info("Error getting project version for ${project.projectDir} using SVN!", e)
            // Don't set the project version
            null
          }
        }
        if (version != null) {
          project.version =
            if (project.extensions.josm.versionWithoutLeadingV && version.length >= 2 && version[0] == 'v') {
              version.substring(1)
            } else {
              version
            }
        }
      }

      // Add the repositories defined in the JOSM configuration
      project.extensions.josm.repositories.invoke(project.repositories)

      if (project.extensions.josm.logSkippedTasks) {
        project.logSkippedTasks()
      }
      if (project.extensions.josm.logTaskDuration) {
        project.gradle.taskGraph.logTaskDuration()
      }
      if (project.plugins.hasPlugin(JacocoPlugin::class.java) && project.extensions.josm.logJacocoCoverage) {
        project.tasks.withType(JacocoReport::class.java) { it.logCoverage() }
      }
    }

    project.configurations.getByName("implementation").setupAsMainConfiguration(project)
    project.setupJosmTasks()

    if (sourceDirectorySetFactory == null) {
      project.logger.warn("No source directory set factory given! The i18n source sets are not configured.")
    } else {
      project.convention.java.sourceSets.all {
        it.setup(project, sourceDirectorySetFactory)
      }
    }
  }
}
