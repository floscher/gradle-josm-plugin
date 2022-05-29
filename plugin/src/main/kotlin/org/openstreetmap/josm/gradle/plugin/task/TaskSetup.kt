package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.openstreetmap.josm.gradle.plugin.MainConfigurationSetup
import org.openstreetmap.josm.gradle.plugin.io.PluginInfo
import org.openstreetmap.josm.gradle.plugin.task.GeneratePluginList.Companion.register
import org.openstreetmap.josm.gradle.plugin.task.RenameArchiveFile.Companion.register
import org.openstreetmap.josm.gradle.plugin.task.github.CreateGithubReleaseTask
import org.openstreetmap.josm.gradle.plugin.task.github.PublishToGithubReleaseTask
import org.openstreetmap.josm.gradle.plugin.task.gitlab.ReleaseToGitlab
import org.openstreetmap.josm.gradle.plugin.util.josm
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * This method sets up all the [Task]s (and [Configuration]s) for a given project that should be there by default.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public fun Project.setupJosmTasks(mainConfigSetup: MainConfigurationSetup) {

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
    tasks.register(
      "${mainConfigSetup.mainSourceSet.compileJavaTaskName}_minJosm",
      CustomJosmVersionCompile::class.java,
      { project.extensions.josm.manifest._minJosmVersion.get() },
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
  val archiverTask: TaskProvider<Jar> = project.tasks.named(sourceSetJosmPlugin.jarTaskName, Jar::class.java)
  val distDir = project.layout.buildDirectory.map { it.dir("dist") }
  val localDistDir = project.layout.buildDirectory.map { it.dir("localDist") }

  val localDistJarTask = project.tasks.register<RenameArchiveFile>(
    "localDistJar",
    archiverTask,
    project.provider { project.tasks.named("generateManifest", GenerateJarManifest::class.java).get() }, // TODO: Don't reference by name
    localDistDir,
    project.provider { project.extensions.josm.pluginName + "-dev" }
  )
  val localDistTask = project.tasks.register<GeneratePluginList>(
    "localDist",
    { relPath: String -> sourceSetJosmPlugin.resources.srcDirs.map { it.resolve(relPath) }.firstOrNull { it.exists() } }
  ) {
    description = "Creates a local plugin update site containing just the current development state of the ${project.extensions.josm.pluginName} plugin"
    outputFile.set(localDistDir.map { it.file("list") })
    plugins.add(localDistJarTask.map { localDistJarTask ->
      PluginInfo(
        localDistJarTask.fileBaseName.get(),
        localDistJarTask.archiveFile.get().asFile.toURI(),
        project.provider { project.tasks.named("generateManifest", GenerateJarManifest::class.java).get() } // TODO: Don't reference by name
          ?.flatMap { it.predefinedAttributes }
          ?.get()
          ?: mapOf()
      )
    })
    versionSuffix.set("#${DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now())}")
  }

  val distTask = project.tasks.register<RenameArchiveFile>(
    "dist",
    archiverTask,
    project.provider { project.tasks.named("generateManifest", GenerateJarManifest::class.java).get() }, // TODO: Don't reference by name
    distDir,
    project.provider { project.extensions.josm.pluginName }
  )

  project.tasks.getByName(sourceSetJosmPlugin.jarTaskName).finalizedBy(distTask, localDistTask)
}

private fun setupI18nTasks(project: Project, sourceSetJosmPlugin: SourceSet) {

  // Generates a *.pot file out of all *.java and *.kt source files
  project.tasks.create(
    "generatePot",
    GeneratePot::class.java,
    project.provider {
      sourceSetJosmPlugin.allSource.filter { it.isFile && it.extension.lowercase() in setOf("java", "kt") }.asFileTree.files
        .minus(sourceSetJosmPlugin.resources.asFileTree.files)
    }
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
