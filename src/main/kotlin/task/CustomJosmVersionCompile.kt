package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.openstreetmap.josm.gradle.plugin.createJosm
import org.openstreetmap.josm.gradle.plugin.isJosmDependency
import java.io.File
import javax.inject.Inject

open class CustomJosmVersionCompile
  @Inject
  constructor(customVersion: String, findNextVersion: Boolean, sourceSet: SourceSet, additionalClasspath: FileCollection): JavaCompile() {

  private lateinit var customJosm : Dependency

  init {
    classpath = additionalClasspath
    source.add(sourceSet.java)
    destinationDir =  File(project.buildDir, "classes/java/${sourceSet.name}_${customVersion}")

    project.gradle.taskGraph.whenReady {
      if (it.hasTask(this)) {
        customJosm = project.dependencies.createJosm(project, customVersion, findNextVersion)
        val customConfig = project.configurations.detachedConfiguration(*
        project.configurations.getByName(sourceSet.compileClasspathConfigurationName).dependencies
          .filterNot { project.dependencies.isJosmDependency(it) }
          .plus(customJosm)
          .toTypedArray()
        )
        classpath += customConfig

        doFirst {
          logger.lifecycle("Compiling against JOSM ${customJosm.version}…")
        }
      }
    }
  }
}
