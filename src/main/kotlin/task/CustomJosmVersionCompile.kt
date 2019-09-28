package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.createJosmDependencyFuzzy
import org.openstreetmap.josm.gradle.plugin.util.isJosm
import java.io.File
import javax.inject.Inject

/**
 * This task compiles the given source set against a specific JOSM version.
 */
@ExperimentalUnsignedTypes
open class CustomJosmVersionCompile
  @Inject
  constructor(customVersionProvider: () -> String, findNextVersion: Boolean, sourceSet: SourceSet, additionalClasspath: FileCollection): JavaCompile() {

  private lateinit var customJosm : Dependency

  init {
    group = "JOSM"
    classpath = additionalClasspath

    project.afterEvaluate {
      source(sourceSet.java)

      val customVersion = customVersionProvider.invoke()
      description = "Compile the JOSM plugin against ${ if (findNextVersion) "the first available JOSM version since" else "JOSM version" } $customVersion"
      destinationDir =  File(project.buildDir, "classes/java/${sourceSet.name}_$customVersion")

      project.gradle.taskGraph.whenReady { graph ->
        if (graph.hasTask(this)) {
          this.customJosm = if (findNextVersion && customVersion.matches(Regex("[1-9][0-9]+"))) {
            project.createJosmDependencyFuzzy(customVersion.toUInt())
          } else {
            project.dependencies.createJosm(customVersion)
          }
          val customConfig = project.configurations.detachedConfiguration(*
            project.configurations.getByName(sourceSet.compileClasspathConfigurationName).dependencies
              .filterNot { it.isJosm() }
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
}
