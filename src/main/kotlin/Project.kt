package org.openstreetmap.josm.gradle.plugin

/**
 * Extends [Project] with methods specific to JOSM development.
 */

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
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
fun Project.getNextJosmVersion(startVersion: String?): Dependency {
  var cause: Throwable? = null
  val startVersionInt = startVersion?.toIntOrNull()
  if (startVersion == null) {
    throw GradleException("Could not add dependency on min. required JOSM version when null is given as version number! Set JosmManifest.minJosmVersion to fix this.")
  } else if (startVersionInt == null) {
    try {
      return resolveJosm(startVersion)
    } catch (e: GradleException) {
      cause = e
    }
  } else {
    for (i in startVersionInt .. startVersionInt + 49) {
      try {
        return resolveJosm(i.toString())
      } catch (e: GradleException) {
        cause = e
      }
    }
  }
  throw GradleException("Could not determine the minimum required JOSM version from the given version number '$startVersion'", if (cause != null) cause else Exception())
}

private fun Project.resolveJosm(version: String): Dependency {
  val dep = dependencies.create("org.openstreetmap.josm:josm:$version")
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
fun Project.getAllRequiredJosmPlugins(directlyRequiredPlugins: Collection<String>): Set<Dependency> {
  logger.lifecycle("Resolving required JOSM plugins…")
  val result = getAllRequiredJosmPlugins(0, mutableSetOf(), directlyRequiredPlugins.toSet())
  logger.lifecycle("{} JOSM plugins are required: {}", result.size, result.map { it.name }.joinToString(", "))
  return result
}

private fun Project.getAllRequiredJosmPlugins(recursionDepth: Int, alreadyResolvedPlugins: MutableSet<String>, directlyRequiredPlugins: Set<String>): Set<Dependency> {
  val realRecursionDepth = max(0, recursionDepth)
  if (realRecursionDepth >= extensions.josm.maxPluginDependencyDepth) {
    throw GradleException(
      "Dependency tree of required JOSM plugins is too deep (>= %d steps). Aborting resolution of required JOSM plugins."
        .format(extensions.josm.maxPluginDependencyDepth)
    )
  }

  val indentation = "  ".repeat(maxOf(0, recursionDepth))
  val result = HashSet<Dependency>()
  for (pluginName in directlyRequiredPlugins) {
    val conf = configurations.detachedConfiguration()
    if (alreadyResolvedPlugins.contains(pluginName)) {
      logger.info("{}* {} (see above for dependencies)", indentation, pluginName)
    } else {
      val dep = dependencies.create("org.openstreetmap.josm.plugins:$pluginName:") as ExternalModuleDependency
      dep.setChanging(true)
      conf.dependencies.add(dep)
      val resolvedFiles = conf.fileCollection(dep).files
      alreadyResolvedPlugins.add(pluginName)
      for (file in resolvedFiles) {
        logger.info("{}* {}", indentation, pluginName);
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
val ExtensionContainer.josm
  get() = getByType(JosmPluginExtension::class.java)

/**
 * Convenience method to access the Java plugin convention.
 * @return the [JavaPluginConvention] of the project
 */
val Convention.java
  get() = getPlugin(JavaPluginConvention::class.java)
