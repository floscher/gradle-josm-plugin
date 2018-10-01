package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.openstreetmap.josm.gradle.plugin.java
import org.openstreetmap.josm.gradle.plugin.josm
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
fun Project.setupJosmTasks() {
  val sourceSetJosmPlugin = project.convention.java.sourceSets.getByName("main")
  val configurationRequiredPlugin = project.configurations.getByName("requiredPlugin")
  val configurationPackIntoJar = project.configurations.getByName("packIntoJar")

  tasks.create("listJosmVersions", ListJosmVersions::class.java)

  tasks.create("cleanJosm", CleanJosm::class.java)

  // Init JOSM preferences.xml file
  val initJosmPrefs = tasks.create("initJosmPrefs", InitJosmPrefs::class.java)

  val writePluginConfig = tasks.create("writePluginConfig", WriteRequiredPluginConfig::class.java)

  // Copy all needed JOSM plugin *.jar files into the directory in {@code $JOSM_HOME}
  val updateJosmPlugins = tasks.create("updateJosmPlugins", Sync::class.java) {
    it.description = "Put all needed plugin *.jar files into the plugins directory. This task copies files into the temporary JOSM home directory."
    it.dependsOn(initJosmPrefs)
    it.dependsOn(writePluginConfig)
    it.rename("(.*)-(SNAPSHOT)?\\.jar", "$1.jar")
  }
  afterEvaluate {
    updateJosmPlugins.from(it.tasks.getByName("dist"))
    updateJosmPlugins.from(configurationRequiredPlugin)
    updateJosmPlugins.into(File(extensions.josm.tmpJosmUserdataDir, "plugins"))
  }

  // Standard run-task
  tasks.create("runJosm", RunJosmTask::class.java)
  tasks.create("debugJosm", DebugJosm::class.java)

  tasks.create("${sourceSetJosmPlugin.compileJavaTaskName}_latestJosm", CustomJosmVersionCompile::class.java, "latest", false, sourceSetJosmPlugin, configurationRequiredPlugin + configurationPackIntoJar)
  tasks.create("${sourceSetJosmPlugin.compileJavaTaskName}_testedJosm", CustomJosmVersionCompile::class.java, "tested", false, sourceSetJosmPlugin, configurationRequiredPlugin + configurationPackIntoJar)
  project.afterEvaluate {
    tasks.create("${sourceSetJosmPlugin.compileJavaTaskName}_minJosm", CustomJosmVersionCompile::class.java, project.extensions.josm.manifest.minJosmVersion as String, true, sourceSetJosmPlugin, configurationRequiredPlugin + configurationPackIntoJar)
  }

  setupI18nTasks(this, sourceSetJosmPlugin)
  setupPluginDistTasks(this, sourceSetJosmPlugin)
}

private fun setupPluginDistTasks(project: Project, sourceSetJosmPlugin: SourceSet) {
  val archiverTask = project.tasks.withType(AbstractArchiveTask::class.java).getByName(sourceSetJosmPlugin.jarTaskName)
  val distDir = File(project.buildDir, "dist")
  val localDistDir = File(project.buildDir, "localDist")

  val localDistTask = project.tasks.create("localDist", GeneratePluginList::class.java) {
    it.group = "JOSM"
    it.outputFile = File(localDistDir, "list")
    it.description = "Generate a local plugin site."
    it.dependsOn(archiverTask)

    project.afterEvaluate {_ ->
      val localDistReleaseFile = File(localDistDir, project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName + "-dev.${archiverTask.extension}")
      it.description += String.format(
        "Add '%s' as plugin site in JOSM preferences (expert mode) and you'll be able to install the current development state as plugin '%s'",
        it.outputFile.toURI().toURL(),
        localDistReleaseFile.nameWithoutExtension
      )

      // Make sure translations are available as *.lang files (if there are any)
      project.extensions.josm.manifest.langCompileTask?.let { langTask ->
        it.dependsOn(langTask)
      }

      it.doFirst { _ ->
        project.copy {
          it.from(archiverTask)
          it.into(localDistDir)
          it.duplicatesStrategy = DuplicatesStrategy.FAIL
          it.rename { localDistReleaseFile.name }
        }
        it.addPlugin(
          localDistReleaseFile.nameWithoutExtension,
          project.extensions.josm.manifest.createJosmPluginJarManifest(),
          localDistReleaseFile.toURI().toURL()
        )
      }

      it.doLast { _ ->
        it.logger.lifecycle(
          "A local JOSM update site for plugin '{}' (version {}) has been written to {}",
          localDistReleaseFile.nameWithoutExtension,
          project.version,
          it.outputFile.absolutePath
        )
      }
    }
    it.iconBase64Provider = { iconPath ->
      try {
        val iconFile = sourceSetJosmPlugin.resources.srcDirs.map { File(it, iconPath) }.filter { it.exists() }.firstOrNull()
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
        it.logger.lifecycle("Error reading icon file!", e)
        null
      }
    }
  }

  val distTask = project.tasks.create("dist", Sync::class.java) {
    it.from(project.tasks.getByName(sourceSetJosmPlugin.jarTaskName))
    it.into(distDir)
    it.duplicatesStrategy = DuplicatesStrategy.FAIL
    project.afterEvaluate { _ ->
      val fileName = project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName + ".${archiverTask.extension}"
      it.doFirst { _ ->
        it.rename { fileName }
      }
      it.doLast {
        it.logger.lifecycle(
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

  // Generate a list of all files in the main Java source set
  val genSrcFileList = project.tasks.create(
    "generateSrcFileList",
    GenerateFileList::class.java,
    File(project.buildDir, "srcFileList.txt"),
    sourceSetJosmPlugin
  )

  project.tasks.create(
    "generatePot",
    GeneratePot::class.java,
    genSrcFileList
  )

  project.tasks.create("transifexDownload", TransifexDownload::class.java)
}