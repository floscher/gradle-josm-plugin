package org.openstreetmap.josm.gradle.plugin.util

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension

/** Constant containing the group used for JMapViewer artifacts in Maven repos */
const val GROUP_JMAPVIEWER = "org.openstreetmap.jmapviewer"
/** Constant containing the group used for JOSM artifacts in Maven repos */
const val GROUP_JOSM = "org.openstreetmap.josm"
/** Constant containing the group used for JOSM plugins in Maven repos */
const val GROUP_JOSM_PLUGIN = "$GROUP_JOSM.plugins"
/** Constant containing the group used for JOSM metadata (e.g. plugin list) in Maven repos */
const val GROUP_METADATA = "$GROUP_JOSM.metadata"

/** Constant containing the artifact name of JOSM, namely `josm` */
const val ARTIFACT_JOSM = "josm"
/** Constant containing the artifact name of the JOSM unittest library, namely `josm-unittest` */
const val ARTIFACT_JOSM_UNITTEST = "josm-unittest"
/** Constant containing the artifact name of the JOSM plugin list, namely `plugin-list` */
const val ARTIFACT_PLUGIN_LIST = "plugin-list"
/** Constant containing the version number used for snapshot versions of artifacts */
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
    } catch (e: IllegalStateException) {
      // This exception is thrown in unit tests, as ProjectBuilder does not instantiate settings.
      // See https://github.com/gradle/gradle/issues/18475 for bug report. FIXME remove when #18475 is fixed.
      logger.info("JOSM version $version can not be found in the available repositories.", e)
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
 * Exclude the dependency on JOSM from the given configuration (if present)
 */
fun Configuration.excludeJosm(): Configuration = this.exclude(mapOf(ExcludeRule.GROUP_KEY to GROUP_JOSM, ExcludeRule.MODULE_KEY to ARTIFACT_JOSM))
