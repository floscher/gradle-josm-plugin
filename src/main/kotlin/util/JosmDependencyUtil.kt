package org.openstreetmap.josm.gradle.plugin.util

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension

const val GROUP_JOSM = "org.openstreetmap.josm"
const val GROUP_JOSM_PLUGIN = "$GROUP_JOSM.plugins"
const val GROUP_METADATA = "$GROUP_JOSM.metadata"

const val ARTIFACT_JOSM = "josm"
const val ARTIFACT_PLUGIN_LIST = "plugin-list"
const val VERSION_SNAPSHOT = "1.0-SNAPSHOT"

/**
 * Finds the next JOSM version available in the repositories known to Gradle, starting with a given version number.
 *
 * The parameter [versionFuzziness] determines, how many versions are checked, starting at [version].
 * If [versionFuzziness] is not given as argument, the value of [JosmPluginExtension.josmVersionFuzziness] is used.
 *
 * @return the next available JOSM dependency
 * @throws [GradleException] if no suitable JOSM version can be found.
 */
@ExperimentalUnsignedTypes
@Throws(GradleException::class)
fun Project.createJosmDependencyFuzzy(version: UInt, versionFuzziness: UInt = this.extensions.josm.josmVersionFuzziness.toUInt()): Dependency {
  var cause: Throwable? = null
  for (v in version until version + versionFuzziness) {
    val dep = dependencies.createJosm(v.toString())
    val conf = configurations.detachedConfiguration(dep)
    try {
      conf.resolve()
      logger.lifecycle("Using JOSM version $version as minimum version to compile against.")
      return dep
    } catch (e: GradleException) {
      logger.info("JOSM version $version can not be found in the available repositories.")
      cause = e
    }
  }
  throw GradleException("Could not determine the minimum required JOSM version from the given version number '$version'", cause)
}

/**
 * Creates a dependency on JOSM using the given version number
 * @param [version] the version number for JOSM (latest and tested are special versions that are [ExternalModuleDependency.isChanging])
 * @return the dependency as created by [DependencyHandler.create]
 */
fun DependencyHandler.createJosm(version: String): ExternalModuleDependency =
  (this.create("$GROUP_JOSM:$ARTIFACT_JOSM:$version") as ExternalModuleDependency)
    .also { it.isChanging = version == "tested" || version == "latest" }

fun DependencyHandler.createJosmPlugin(name: String): ExternalModuleDependency =
  (this.create("$GROUP_JOSM_PLUGIN:$name:$VERSION_SNAPSHOT") as ExternalModuleDependency).also { it.isChanging = true }

/**
 * @return true iff the given dependency contains JOSM (the group and name of the dependency are checked to determine this)
 */
fun Dependency.isJosm(): Boolean = this.group == GROUP_JOSM && this.name == ARTIFACT_JOSM
