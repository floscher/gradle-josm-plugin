package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.work.InputChanges
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.createJosmDependencyFuzzy
import org.openstreetmap.josm.gradle.plugin.util.excludeJosm
import java.io.File
import javax.inject.Inject

/**
 * This task compiles the given source set against a specific JOSM version.
 */
@ExperimentalUnsignedTypes
open class CustomJosmVersionCompile
  @Inject
  constructor(private val customVersionProvider: () -> String, private val findNextVersion: Boolean, private val sourceSet: SourceSet, private val additionalClasspath: Set<Configuration>): JavaCompile() {

  private lateinit var customVersion: String

  @TaskAction
  final override fun compile(inputs: InputChanges) {
    classpath += project.configurations.getByName(sourceSet.compileClasspathConfigurationName).copy().excludeJosm()
    additionalClasspath.forEach {
      classpath += it.copy()
    }
    val customJosm = if (findNextVersion && customVersion.matches(Regex("[1-9][0-9]+"))) {
      project.createJosmDependencyFuzzy(customVersion.toUInt())
    } else {
      project.dependencies.createJosm(customVersion)
    }
    classpath += project.configurations.detachedConfiguration(customJosm)

    logger.lifecycle("Compiling against JOSM ${customJosm.version}…")

    super.compile(inputs)
  }

  init {
    group = "JOSM"

    project.afterEvaluate {
      source(sourceSet.java)
      customVersion = customVersionProvider.invoke()

      description = "Compile the JOSM plugin against ${ if (findNextVersion) "the first available JOSM version since" else "JOSM version" } $customVersion"
      destinationDir =  File(project.buildDir, "classes/java/${sourceSet.name}_$customVersion")

      classpath = project.files() // empty, will be filled later
    }
  }
}
