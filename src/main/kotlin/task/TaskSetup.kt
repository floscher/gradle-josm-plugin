package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Sync
import org.openstreetmap.josm.gradle.plugin.java
import org.openstreetmap.josm.gradle.plugin.josm
import java.io.File

fun Project.setupJosmTasks() {
  tasks.create("cleanJosm", CleanJosm::class.java)

  // Init JOSM preferences.xml file
  val initJosmPrefs = tasks.create("initJosmPrefs", InitJosmPrefs::class.java)

  // Copy all needed JOSM plugin *.jar files into the directory in {@code $JOSM_HOME}
  val updateJosmPlugins = tasks.create("updateJosmPlugins", Sync::class.java, {
    it.description = "Put all needed plugin *.jar files into the plugins directory. This task copies files into the temporary JOSM home directory."
    it.dependsOn(initJosmPrefs)
  })
  afterEvaluate {
    updateJosmPlugins.into(File(extensions.josm.tmpJosmUserdataDir, "plugins"))
    // the rest of the configuration (e.g. from where the files come, that should be copied) is done later (e.g. in the file `PluginTaskSetup.java`)
  }

  // Standard run-task
  tasks.create("runJosm", RunJosmTask::class.java)
  tasks.create("debugJosm", DebugJosm::class.java)

  setupI18nTasks(this)
}

private fun setupI18nTasks(project: Project) {

  // Generate a list of all files in the main Java source set
  val genSrcFileList = project.tasks.create("generateSrcFileList", GenerateFileList::class.java, {
    it.outFile = File(project.buildDir, "srcFileList.txt")
    it.srcSet = project.convention.java.sourceSets.getByName("main")
  })

  project.tasks.create("generatePot", GeneratePot::class.java, {
    it.fileListGenTask = genSrcFileList
  })

  project.tasks.create("transifexDownload", TransifexDownload::class.java)
}
