package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.openstreetmap.josm.gradle.plugin.api.gitlab.gitlabRepository
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.task.setupJosmTasks
import org.openstreetmap.josm.gradle.plugin.util.GROUP_JOSM_PLUGIN
import org.openstreetmap.josm.gradle.plugin.util.java
import org.openstreetmap.josm.gradle.plugin.util.josm

/**
 * Main class of the plugin, sets up the custom configurations <code>requiredPlugin</code> and <code>packIntoJar</code>,
 * the additional repositories and the custom tasks.
 */
class JosmPlugin: Plugin<Project> {

  /**
   * Set up the JOSM plugin.
   *
   * Creates the tasks this plugin provides, defines the `josm` extension, adds the repositories where JOSM specific dependencies can be found.
   * Overrides [Plugin.apply].
   */
  override fun apply(project: Project) {

    // Apply the Java plugin if not available, because we rely on the `jar` task
    if (project.plugins.findPlugin(JavaPlugin::class.java) == null) {
      project.apply { it.plugin(JavaPlugin::class.java) }
    }

    // Define 'josm' extension
    project.extensions.create("josm", JosmPluginExtension::class.java, project)

    val jarTask = project.tasks.withType(Jar::class.java).getByName("jar")
    jarTask.outputs.upToDateWhen { false }
    jarTask.doFirst { task ->
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

    val mainConfigurationSetup = MainConfigurationSetup(project, project.extensions.java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME))
    project.setupJosmTasks(mainConfigurationSetup)

    project.extensions.java.sourceSets.all {
      it.setup(project)
    }

    project.afterEvaluate {
      if (project.version == Project.DEFAULT_VERSION) {
        try {
          GitDescriber(project.projectDir).describe(markSnapshots = true, trimLeading = true)
        } catch (e: Exception) {
          project.logger.info("Error getting project version for ${project.projectDir.absolutePath} using git!", e)
          try {
            // Fall back to SVN revision if `git describe` does not work
            SvnDescriber(project.projectDir).describe(markSnapshots = true, trimLeading = true)
          } catch (e: Exception) {
            project.logger.info("Error getting project version for ${project.projectDir.absolutePath} using SVN!", e)
            // Don't set the project version
            val msg = {
              project.logger.warn("""
                WARNING: Could not detect the project version, you are probably not building inside a git repository!
                WARNING: The project version is currently the default value `${project.version}`.
                WARNING: To change the version number, either build in a git-repository or set the version manually by adding the line `project.version = "1.2.3"` to the Gradle buildscript.
              """.trimIndent())
            }
            msg.invoke() // Immediately print warning …
            project.gradle.buildFinished { // … and print warning when build is finished.
              msg.invoke()
            }

            null
          }
        }?.let { // If a version can be determined through VCS systems
          project.version = it
        }
      }

      // Add the publishing repositories defined in the JOSM configuration
      if (project.plugins.hasPlugin(MavenPublishPlugin::class.java)) {
        val prevPublishRepos = project.extensions.josm.publishRepositories
        project.extensions.josm.publishRepositories = {
          it.gitlabRepository("gitlab", project)
          prevPublishRepos.invoke(it)
        }
        project.extensions.josm.publishRepositories.invoke(project.extensions.getByType(PublishingExtension::class.java).repositories)
        project.extensions.getByType(PublishingExtension::class.java).publications { publications ->
          publications.create("josmPlugin", MavenPublication::class.java) {
            it.groupId = GROUP_JOSM_PLUGIN
            it.artifactId = project.extensions.josm.pluginName
            project.afterEvaluate { project ->
              it.version = project.version.toString()
            }
            it.from(project.components.getByName("java"))
          }
        }
      } else {
        project.logger.lifecycle("Note: Add the `maven-publishing` Gradle plugin to your Gradle buildscript to publish your project to a Maven repository.")
      }

      // Add the repositories defined in the JOSM configuration
      project.extensions.josm.repositories.invoke(project.repositories)

      mainConfigurationSetup.afterEvaluate()

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
  }
}
