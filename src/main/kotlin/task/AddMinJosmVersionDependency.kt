package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.getNextJosmVersion
import org.openstreetmap.josm.gradle.plugin.java
import org.openstreetmap.josm.gradle.plugin.josm

/**
 * Add the dependencies of the implementation configuration of one source set (mainSourceSet) to the
 * implementation configuration of another source set (minJosmVersionSourceSet).
 *
 * The version of the JOSM dependency is set to [JosmManifest.minJosmVersion]. When this task runs, it is checked,
 * if that version is available, if not a later version is chosen according to [getNextJosmVersion].
 */
open class AddMinJosmVersionDependency: DefaultTask() {
  private val IS_JOSM_DEPENDENCY = { dep : Dependency -> dep.name == "josm" && dep.group == "org.openstreetmap.josm" }

  @Internal
  private lateinit var mainConfiguration: Configuration

  @Internal
  private lateinit var minJosmVersionConfiguration: Configuration

  /**
   * Initialize the task with the two source sets on which it should operate.
   * @param mainSourceSet the source set that serves as template, its implementation configuration is copied over
   * @param minJosmVersionSourceSet the source set that is copied from [mainSourceSet],
   */
  fun init(mainSourceSet: SourceSet, newSourceSetName: String) {
    val minJosmVersionSourceSet = project.convention.java.sourceSets.create(newSourceSetName, { srcSet ->
      project.afterEvaluate {
        srcSet.java {
          it.setSrcDirs(mainSourceSet.java.srcDirs)
          it.setIncludes(mainSourceSet.java.includes)
          it.setExcludes(mainSourceSet.java.excludes)
        }
      }
      project.tasks.getByName(srcSet.classesTaskName).also {
        it.group = "JOSM"
        it.description = "Try to compile against the version of JOSM that is specified in the manifest (${project.extensions.josm.manifest.minJosmVersion}) as the minimum compatible version."
      }
    })

    mainConfiguration = project.configurations.getByName(mainSourceSet.implementationConfigurationName)
    minJosmVersionConfiguration = project.configurations.getByName(minJosmVersionSourceSet.implementationConfigurationName)
    description = "Adds dependency for the minimum required JOSM version to the configuration `${minJosmVersionConfiguration.name}`."

    // Add dependency to minimum JOSM version from manifest
    project.dependencies.add(minJosmVersionSourceSet.implementationConfigurationName, "org.openstreetmap.josm:josm:${project.extensions.josm.manifest.minJosmVersion}")
    // Execute this task before the `compileMinJosmVersionJava` task
    project.tasks.getByName(minJosmVersionSourceSet.compileJavaTaskName).dependsOn(this)

    project.afterEvaluate {
      // Add direct dependencies of implementation configuration to minJosmVersionImplementation configuration (except JOSM)
      mainConfiguration.dependencies.filterNot(IS_JOSM_DEPENDENCY).forEach {
        project.dependencies.add(minJosmVersionSourceSet.implementationConfigurationName, it)
      }
      // Let `minJosmVersionImplementation` configuration extend from the same configurations as the `implementation` configuration does
      minJosmVersionConfiguration.setExtendsFrom(mainConfiguration.extendsFrom)
    }

    doFirst {
      val minJosmVersion = project.extensions.josm.manifest.minJosmVersion
      it.logger.lifecycle("Manifest requests minimum JOSM version $minJosmVersion. Searching next version available for download…")

      // Update version of JOSM in `minJosmVersionImplementation` configuration to the next available
      minJosmVersionConfiguration.dependencies.removeIf(IS_JOSM_DEPENDENCY)
      project.dependencies.add(minJosmVersionConfiguration.name, project.getNextJosmVersion(minJosmVersion))
    }
  }
}
