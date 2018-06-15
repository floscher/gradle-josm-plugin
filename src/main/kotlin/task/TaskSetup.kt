package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.openstreetmap.josm.gradle.plugin.java
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File

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
}

private fun setupI18nTasks(project: Project, sourceSetJosmPlugin: SourceSet) {

  // Generate a list of all files in the main Java source set
  val genSrcFileList = project.tasks.create("generateSrcFileList", GenerateFileList::class.java, {
    it.outFile = File(project.buildDir, "srcFileList.txt")
    it.srcSet = sourceSetJosmPlugin
  })

  project.tasks.create("generatePot", GeneratePot::class.java, {
    it.fileListGenTask = genSrcFileList
  })

  project.tasks.create("transifexDownload", TransifexDownload::class.java)
}
