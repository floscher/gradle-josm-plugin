package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.openstreetmap.josm.gradle.plugin.MainConfigurationSetup
import org.openstreetmap.josm.gradle.plugin.task.github.CreateGithubReleaseTask
import org.openstreetmap.josm.gradle.plugin.task.github.PublishToGithubReleaseTask
import org.openstreetmap.josm.gradle.plugin.task.gitlab.ReleaseToGitlab
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64

/**
 * This method sets up all the [Task]s (and [Configuration]s) for a given project that should be there by default.
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun Project.setupJosmTasks(mainConfigSetup: MainConfigurationSetup) {

  tasks.create("listJosmVersions", ListJosmVersions::class.java)

  val initJosmPrefs = tasks.register("initJosmPrefs", InitJosmPreferences::class.java)

  val cleanJosm = project.tasks.register("cleanJosm", CleanJosm::class.java)
  // Standard run-tasks
  project.tasks.register("runJosm", RunJosmTask::class.java, cleanJosm, initJosmPrefs)
  project.tasks.register("debugJosm", DebugJosm::class.java, cleanJosm, initJosmPrefs)

  listOf("latest", "tested").forEach { version ->
    tasks.create(
      "${mainConfigSetup.mainSourceSet.compileJavaTaskName}_${version}Josm",
      CustomJosmVersionCompile::class.java,
      { version },
      false,
      mainConfigSetup.mainSourceSet,
      setOf(mainConfigSetup.requiredPluginConfiguration, mainConfigSetup.packIntoJarConfiguration)
    )
  }
  project.afterEvaluate {
    tasks.create(
      "${mainConfigSetup.mainSourceSet.compileJavaTaskName}_minJosm",
      CustomJosmVersionCompile::class.java,
      { project.extensions.josm.manifest.minJosmVersion as String },
      true,
      mainConfigSetup.mainSourceSet,
      setOf(mainConfigSetup.requiredPluginConfiguration, mainConfigSetup.packIntoJarConfiguration)
    )

    tasks.create(
      "releaseToGitlab",
      ReleaseToGitlab::class.java,
      { true },
      project.extensions.josm.gitlab.publicationNames
    )
  }

  setupI18nTasks(this, mainConfigSetup.mainSourceSet)
  setupPluginDistTasks(this, mainConfigSetup.mainSourceSet)
  setupGithubReleaseTasks(this)
}

private fun setupPluginDistTasks(project: Project, sourceSetJosmPlugin: SourceSet) {
  val archiverTask = project.tasks.withType(AbstractArchiveTask::class.java).getByName(sourceSetJosmPlugin.jarTaskName)
  val distDir = File(project.buildDir, "dist")
  val localDistDir = File(project.buildDir, "localDist")

  val localDistTask = project.tasks.create("localDist", GeneratePluginList::class.java) { genListTask ->
    genListTask.group = "JOSM"
    genListTask.outputFile = File(localDistDir, "list")
    genListTask.description = "Generate a local plugin site."
    genListTask.dependsOn(archiverTask)

    project.afterEvaluate {
      val localDistReleaseFile = File(localDistDir, project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName + "-dev.${archiverTask.archiveExtension.get()}")
      genListTask.description += String.format(
        "Add '%s' as plugin site in JOSM preferences (expert mode) and you'll be able to install the current development state as plugin '%s'",
        genListTask.outputFile.toURI().toURL(),
        localDistReleaseFile.nameWithoutExtension
      )

      genListTask.doFirst {
        project.copy {
          it.from(archiverTask)
          it.into(localDistDir)
          it.duplicatesStrategy = DuplicatesStrategy.FAIL
          it.rename { localDistReleaseFile.name }
        }
        genListTask.addPlugin(
          localDistReleaseFile.nameWithoutExtension,
          project.extensions.josm.manifest.createJosmPluginJarManifest(),
          localDistReleaseFile.toURI().toURL()
        )
      }

      genListTask.doLast {
        it.logger.lifecycle(
          "A local JOSM update site for plugin '{}' (version {}) has been written to {}",
          localDistReleaseFile.nameWithoutExtension,
          project.version,
          genListTask.outputFile.absolutePath
        )
      }
    }
    genListTask.iconBase64Provider = { iconPath ->
      try {
        val iconFile = sourceSetJosmPlugin.resources.srcDirs.map { File(it, iconPath) }.firstOrNull { it.exists() }
        if (iconFile != null) {
          val contentType = Files.probeContentType(Paths.get(iconFile.toURI()))
            ?: FileInputStream(iconFile).use {
              URLConnection.guessContentTypeFromStream(it)
            }
          "data:$contentType;base64,${Base64.getEncoder().encodeToString(iconFile.readBytes())}"
        } else {
          null
        }
      } catch (e: IOException) {
        genListTask.logger.lifecycle("Error reading icon file!", e)
        null
      }
    }
  }

  val distTask = project.tasks.create("dist", Sync::class.java) { distTask ->
    distTask.from(project.tasks.getByName(sourceSetJosmPlugin.jarTaskName))
    distTask.into(distDir)
    distTask.duplicatesStrategy = DuplicatesStrategy.FAIL
    project.afterEvaluate {
      val fileName = project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName + ".${archiverTask.archiveExtension.get()}"
      distTask.doFirst {
        distTask.rename { fileName }
      }
      distTask.doLast {
        distTask.logger.lifecycle(
          "Distribution {} (version {}) has been written into {}",
          fileName,
          project.version,
          distDir.absolutePath
        )
      }
    }
  }

  project.tasks.getByName(sourceSetJosmPlugin.jarTaskName).finalizedBy(distTask, localDistTask)
}

private fun setupI18nTasks(project: Project, sourceSetJosmPlugin: SourceSet) {

  // Generates a *.pot file out of all *.java source files
  project.tasks.create(
    "generatePot",
    GeneratePot::class.java,
    project.provider { sourceSetJosmPlugin.java.asFileTree.files }
  )

  project.tasks.create("transifexDownload", TransifexDownload::class.java)
}

private fun setupGithubReleaseTasks(project: Project) {

  project.tasks.create("createGithubRelease",
    CreateGithubReleaseTask::class.java) {
      it.description =  "Creates a new GitHub release"
  }

  project.tasks.create("publishToGithubRelease",
    PublishToGithubReleaseTask::class.java) {
      it.description = "Publish a JOSM plugin jar as GitHub release asset " +
        "to a GitHub release"
  }
}

