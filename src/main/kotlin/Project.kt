package org.openstreetmap.josm.gradle.plugin

/**
 * Extends [Project] with methods specific to JOSM development.
 */

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.io.JosmPluginListParser
import java.io.IOException
import java.util.jar.Manifest
import java.util.zip.ZipFile
import kotlin.math.max

/**
 * Finds the next JOSM version available in the repositories known to Gradle, starting with a given version number.
 *
 * If the number can be parsed as an integer, 50 version numbers beginning with the given number are checked.
 * Otherwise only the given version is checked.
 *
 * @return the next available JOSM dependency. A [DependencySet] consisting only of this one dependency.
 * @throws [GradleException] if no suitable JOSM version can be found.
 */
fun Project.getNextJosmVersion(startVersion: String): Dependency {
  val versionsToTest = requireNotNull(startVersion) {
    "Could not add dependency on min. required JOSM version when null is given as version number! Set JosmManifest.minJosmVersion to fix this."
  }
    .toIntOrNull()
    ?.let {
      it..it+49
    }
    ?: listOf(startVersion)

  var cause: Throwable? = null
  for (v in versionsToTest) {
    try {
      return resolveJosm(v.toString())
    } catch (e: GradleException) {
      cause = e
    }
  }

  throw GradleException("Could not determine the minimum required JOSM version from the given version number '$startVersion'", cause ?: Exception())
}

private fun Project.resolveJosm(version: String): Dependency {
  val dep = dependencies.createJosm(version)
  val conf = configurations.detachedConfiguration(dep)
  try {
    conf.resolve()
    logger.lifecycle("Using JOSM version $version as minimum version to compile against.")
    return dep
  } catch (e: GradleException) {
    logger.info("JOSM version $version is not available in the available repositories.")
    throw e
  }
}

fun Project.getVirtualPlugins(): Map<String, List<Pair<String, String>>> = try {
  JosmPluginListParser(this).plugins
    .mapNotNull {
      it.manifestAtts["Plugin-Platform"]?.let { platform ->
        it.manifestAtts["Plugin-Provides"]?.let { provides ->
          Triple(provides, platform, if (it.pluginName.endsWith(".jar")) it.pluginName.substring(0..it.pluginName.length - 5) else it.pluginName)
        }
      }
    }
    .groupBy({ it.first }, { Pair(it.second, it.third) })
} catch (e: IOException) {
  logger.warn("WARN: Virtual plugins cannot be resolved, since the plugin list can't be read from the web!")
  mapOf()
}

/**
 * Resolves the JOSM plugin names given as parameter, using the available repositories for this project.
 * Not only are the given plugin names resolved to Dependencies, but also all JOSM plugins on which these plugins depend.
 *
 * The resolution is aborted, if a dependency chain exceeds 10 plugins (plugin 1 depends on plugin 2 … depends on plugin 10). This limit can be changed by [JosmPluginExtension.maxPluginDependencyDepth]
 * @param [directlyRequiredPlugins] a [Set] of [String]s representing the names of JOSM plugins.
 *   These plugins (and their dependencies) will be resolved
 * @return a set of [Dependency] objects, including the requested plugins, plus all plugins required by the requested
 *   plugins
 */
fun Project.getAllRequiredJosmPlugins(directlyRequiredPlugins: Collection<String>): Set<Dependency> =
  if (directlyRequiredPlugins.isNullOrEmpty()) {
    logger.info("No other JOSM plugins required by this plugin.")

    setOf()
  } else {
    logger.lifecycle("Resolving required JOSM plugins…")
    val result = getAllRequiredJosmPlugins(0, mutableSetOf(), directlyRequiredPlugins.toSet())
    logger.lifecycle(" → {} JOSM {} required: {}", result.size, if (result.size == 1) "plugin is" else "plugins are", result.map { it.name }.sorted().joinToString())

    result
  }

private fun Project.getAllRequiredJosmPlugins(recursionDepth: Int, alreadyResolvedPlugins: MutableSet<String>, directlyRequiredPlugins: Set<String>): Set<Dependency> {
  val realRecursionDepth = max(0, recursionDepth)
  if (realRecursionDepth >= extensions.josm.maxPluginDependencyDepth) {
    throw GradleException(
      "Dependency tree of required JOSM plugins is too deep (>= %d steps). Aborting resolution of required JOSM plugins."
        .format(extensions.josm.maxPluginDependencyDepth)
    )
  }

  val virtualPlugins = getVirtualPlugins()

  val indentation = "  ".repeat(maxOf(0, recursionDepth))
  val result = HashSet<Dependency>()
  for (pluginName in directlyRequiredPlugins) {
    val conf = configurations.detachedConfiguration()
    if (alreadyResolvedPlugins.contains(pluginName)) {
      logger.info("{}* {} (see above for dependencies)", indentation, pluginName)
    } else if (virtualPlugins.containsKey(pluginName)) {
      val suitableImplementation = virtualPlugins.getValue(pluginName).firstOrNull {
        when (it.first.toLowerCase()) {
          "unixoid" -> Os.isFamily(Os.FAMILY_UNIX)
          "osx" -> Os.isFamily(Os.FAMILY_MAC)
          "windows" -> Os.isFamily(Os.FAMILY_WINDOWS) || Os.isFamily(Os.FAMILY_9X) || Os.isFamily(Os.FAMILY_NT)
          else -> false
        }
      }

      if (suitableImplementation == null) {
        logger.warn("WARN: No suitable implementation found for virtual JOSM plugin $pluginName!")
      } else {
        alreadyResolvedPlugins.add(pluginName)
        logger.info("{}* {} (virtual): provided by {}", indentation, pluginName, virtualPlugins.getValue(pluginName).joinToString { "${it.second} for ${it.first}" })
        result.addAll(getAllRequiredJosmPlugins(realRecursionDepth + 1, alreadyResolvedPlugins, setOf(suitableImplementation.second)))
      }
    } else {
      val dep = dependencies.create("org.openstreetmap.josm.plugins:$pluginName:SNAPSHOT") as ExternalModuleDependency
      dep.isChanging = true
      conf.dependencies.add(dep)
      val resolvedFiles = conf.fileCollection(dep).files
      alreadyResolvedPlugins.add(pluginName)
      for (file in resolvedFiles) {
        logger.info("{}* {}", indentation, pluginName)
        ZipFile(file).use {
          val entries = it.entries()
          while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            if ("META-INF/MANIFEST.MF" == zipEntry.name) {
              val requirements = Manifest(it.getInputStream(zipEntry)).mainAttributes.getValue("Plugin-Requires")
                ?.split(";")
                ?.map { it.trim() }
                ?.toSet()
                ?: setOf()
              result.addAll(getAllRequiredJosmPlugins(
                realRecursionDepth + 1,
                alreadyResolvedPlugins,
                requirements
              ))
            }
          }
        }
      }
    }
    result.addAll(conf.dependencies)
  }
  return result
}

/**
 * Creates a dependency onto JOSM using the given version number
 * @param [version] the version number for JOSM (latest and tested are special versions that are [ExternalModuleDependency.isChanging])
 * @return the dependency as created by [DependencyHandler.create]
 */
fun DependencyHandler.createJosm(version: String): Dependency =
  when (version) {
    "latest", "tested" ->
      (create("org.openstreetmap.josm:josm:$version") as ExternalModuleDependency).setChanging(true)
    else ->
      create("org.openstreetmap.josm:josm:$version")
  }

/**
 * @return true iff the given dependency contains JOSM (the group and name of the dependency are checked to determine this)
 */
fun Dependency.isJosmDependency() = this.group == "org.openstreetmap.josm" && this.name == "josm"

/**
 * @return true if a JOSM version of 7841 or later is used that can be configured to use separate directories for cache, preferences and userdata
 */
fun Project.useSeparateTmpJosmDirs(): Boolean {
  val josmVersionNum = extensions.josm.josmCompileVersion?.toIntOrNull()
  return josmVersionNum == null || josmVersionNum >= 7841
}

/**
 * Access method for the `project.josm{}` extension.
 * @return the [JosmPluginExtension] for this project.
 */
val ExtensionContainer.josm : JosmPluginExtension
  get() = getByType(JosmPluginExtension::class.java)

/**
 * Convenience method to access the Java plugin convention.
 * @return the [JavaPluginConvention] of the project
 */
val Convention.java : JavaPluginConvention
  get() = getPlugin(JavaPluginConvention::class.java)
