package org.openstreetmap.josm.gradle.plugin.task

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.execution.commandline.TaskConfigurationException
import org.gradle.work.InputChanges
import org.openstreetmap.josm.gradle.plugin.util.createJosm
import org.openstreetmap.josm.gradle.plugin.util.createJosmDependencyFuzzy
import org.openstreetmap.josm.gradle.plugin.util.excludeJosm
import javax.inject.Inject

/**
 * This task compiles the given source set against a specific JOSM version.
 */
public open class CustomJosmVersionCompile
  @Inject
  constructor(private val customVersionProvider: () -> String?, private val findNextVersion: Boolean, private val sourceSet: SourceSet, private val additionalClasspath: Set<Configuration>): JavaCompile() {

  private val customVersion: String? by lazy { customVersionProvider() }

  @OptIn(ExperimentalUnsignedTypes::class)
  @TaskAction
  final override fun compile(inputs: InputChanges) {
    val customVersion = this.customVersion ?: throw TaskConfigurationException(path, "Custom version not defined!", IllegalStateException())
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

  private val destinationDirectory = project.objects.directoryProperty().also {
    if (customVersion != null) {
      it.convention(
        project.layout.buildDirectory.dir("classes/java/${sourceSet.name}_$customVersion")
      )
    }
  }
  final override fun getDestinationDirectory(): DirectoryProperty = destinationDirectory

  @Internal
  override fun getDescription(): String = if (customVersion == null) {
    "This task will fail, because the custom version is unknown!"
  } else {
    "Compile the JOSM plugin against ${
      if (findNextVersion) "the first available JOSM version since" else "JOSM version"
    } $customVersion"
  }
  final override fun setDescription(description: String?): Nothing = throw UnsupportedOperationException("Can't change task description!")

  init {
    group = "JOSM"
    source(project.provider { sourceSet.java })
    classpath = project.files() // empty, will be filled later
  }
}
