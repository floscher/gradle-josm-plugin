package org.openstreetmap.josm.gradle.plugin.util

/**
 * Extends [Project] with methods specific to JOSM development.
 */

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginExtension
import org.openstreetmap.josm.gradle.plugin.config.JosmManifest
import org.openstreetmap.josm.gradle.plugin.config.JosmPluginExtension
import org.openstreetmap.josm.gradle.plugin.io.JosmPluginListParser
import org.openstreetmap.josm.gradle.plugin.task.GenerateJarManifest
import java.util.jar.Manifest
import java.util.zip.ZipFile

/**
 * Configures the [RepositoryHandler] to hold another repository that appends the artifact name to [Urls.MainJosmWebsite.BASE] to get the artifact's URL.
 */
fun RepositoryHandler.josmPluginList(onlyForConfig: Configuration, dependency: Dependency): IvyArtifactRepository = ivy { repo ->
  repo.url = Urls.MainJosmWebsite.BASE.toURI()
  repo.patternLayout {
    it.artifact("[artifact]")
  }
  repo.metadataSources {
    it.artifact()
  }
  repo.content {
    it.onlyForConfigurations(onlyForConfig.name)
    it.includeModule(dependency.group ?: "", dependency.name)
  }
}

/**
 * When used together with [RepositoryHandler.josmPluginList], this returns the list of JOSM plugins from
 * [https://josm.openstreetmap.de/plugin] (for [withIcons] = `false`) or
 * [https://josm.openstreetmap.de/pluginicons] (for [withIcons] = `true`).
 * @param withIcons determines if the plugin list includes the icons as Base64 or not
 * @return the plugin list as a [Dependency]
 */
fun DependencyHandler.josmPluginList(withIcons: Boolean): ExternalModuleDependency =
  (create("$GROUP_METADATA:$ARTIFACT_PLUGIN_LIST:$VERSION_SNAPSHOT") as ExternalModuleDependency)
    .also { dep ->
      dep.isChanging = true
      dep.artifact {
        it.type = "txt"
        it.name = if (withIcons) {
          Urls.MainJosmWebsite.PATH_PLUGIN_LIST_WITH_ICONS
        } else {
          Urls.MainJosmWebsite.PATH_PLUGIN_LIST
        }
      }
    }

/**
 * @return a map with the virtual plugin names as key, the values are a list of pairs, where the first element
 * is the platform, the second element of the pairs is the name of the real plugin providing the virtual plugin.
 */
fun Project.getVirtualPlugins(): Map<String, List<Pair<String, String>>> = try {
  val parser = JosmPluginListParser(this, true)
  parser
    .plugins
    .mapNotNull {
      it.manifestAtts["Plugin-Platform"]?.let { platform ->
        it.manifestAtts["Plugin-Provides"]?.let { provides ->
          Triple(provides, platform, it.pluginName)
        }
      }
    }
    .groupBy( { it.first }, { Pair(it.second, it.third) })
    .also {
      if (parser.errors.isNotEmpty()) {
        logger.warn("WARN: There were issues parsing the JOSM plugin list:\n * " + parser.errors.joinToString("\n * "))
      }
    }
} catch (e: ResolveException) {
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
@OptIn(ExperimentalUnsignedTypes::class)
fun Project.getAllRequiredJosmPlugins(directlyRequiredPlugins: Collection<String>): Set<Dependency> =
  if (directlyRequiredPlugins.isNullOrEmpty()) {
    logger.info("No other JOSM plugins required by this plugin.")

    setOf()
  } else {
    logger.lifecycle("Resolving required JOSM plugins…")
    val result = getAllRequiredJosmPlugins(0.toUShort(), mutableSetOf(), directlyRequiredPlugins.toSet())
    logger.lifecycle(" → {} JOSM {} required: {}", result.size, if (result.size == 1) "plugin is" else "plugins are", result.map { it.name }.sorted().joinToString())

    result
  }

@ExperimentalUnsignedTypes
private fun Project.getAllRequiredJosmPlugins(recursionDepth: UShort, alreadyResolvedPlugins: MutableSet<String>, directlyRequiredPlugins: Set<String>): Set<Dependency> {
  if (recursionDepth.toInt() >= extensions.josm.maxPluginDependencyDepth) {
    throw GradleException(
      "Dependency tree of required JOSM plugins is too deep (>= %d steps). Aborting resolution of required JOSM plugins."
        .format(extensions.josm.maxPluginDependencyDepth)
    )
  }

  val virtualPlugins = getVirtualPlugins()

  val indentation = "  ".repeat(recursionDepth.toInt())
  val result = HashSet<Dependency>()
  for (pluginName in directlyRequiredPlugins) {
    if (alreadyResolvedPlugins.contains(pluginName)) {
      logger.lifecycle("{}* {} (see above for dependencies)", indentation, pluginName)
    } else if (virtualPlugins.containsKey(pluginName)) {
      val suitableImplementation = virtualPlugins.getValue(pluginName).firstOrNull {
        when (it.first.uppercase()) {
          JosmManifest.Platform.UNIXOID.toString() -> Os.isFamily(Os.FAMILY_UNIX)
          JosmManifest.Platform.OSX.toString() -> Os.isFamily(Os.FAMILY_MAC)
          JosmManifest.Platform.WINDOWS.toString() -> Os.isFamily(Os.FAMILY_WINDOWS) || Os.isFamily(Os.FAMILY_9X) || Os.isFamily(Os.FAMILY_NT)
          else -> false
        }
      }

      if (suitableImplementation == null) {
        logger.warn("WARN: No suitable implementation found for virtual JOSM plugin $pluginName!")
      } else {
        alreadyResolvedPlugins.add(pluginName)
        logger.lifecycle("{}* {} (virtual): provides {}", indentation, pluginName, virtualPlugins.getValue(pluginName).joinToString { "${it.second} for ${it.first}" })
        result.addAll(getAllRequiredJosmPlugins(recursionDepth.inc() , alreadyResolvedPlugins, setOf(suitableImplementation.second)))
      }
    } else {
      val dep = dependencies.createJosmPlugin(pluginName)
      val resolvedFiles = configurations.detachedConfiguration(dep).files
      alreadyResolvedPlugins.add(pluginName)
      val resolvedManifests = resolvedFiles.map { Manifest(ZipFile(it).let { it.getInputStream(it.getEntry(GenerateJarManifest.MANIFEST_PATH)) }) }

      val requiredJava = resolvedManifests
        .mapNotNull { it.mainAttributes.getValue(JosmManifest.Attribute.PLUGIN_MIN_JAVA_VERSION.manifestKey)?.toString()?.toIntOrNull() }
        .minOrNull()
      val pluginVersion = resolvedManifests
        .mapNotNull { it.mainAttributes.getValue(JosmManifest.Attribute.PLUGIN_VERSION.manifestKey)?.toString() }
        .distinct()
        .singleOrNull()
      val currentJava = JavaVersion.current().majorVersion.toIntOrNull()
      if (requiredJava != null && currentJava != null && requiredJava > currentJava) {
        // if any manifest has a minimum Java version larger than the current java version
        logger.lifecycle("{}* {} (ignored): requires Java {} instead of {}", indentation, pluginName, requiredJava, currentJava)
      } else {
        logger.lifecycle("{}* {}{}", indentation, pluginName, if (pluginVersion == null) "" else " ($pluginVersion)")
        result.add(dep)
      }

      resolvedManifests.forEach { manifest ->
        result.addAll(
          getAllRequiredJosmPlugins(
            recursionDepth.inc(),
            alreadyResolvedPlugins,
            manifest.mainAttributes.getValue(JosmManifest.Attribute.PLUGIN_DEPENDENCIES.manifestKey)
              ?.split(";")
              ?.filter { it.isNotBlank() }
              ?.map { it.trim() }
              ?.toSet()
              ?: setOf()
          )
        )
      }
    }
  }
  return result
}

/**
 * Access method for the `project.josm{}` extension.
 * @return the [JosmPluginExtension] for this project.
 */
val ExtensionContainer.josm : JosmPluginExtension
  get() = getByType(JosmPluginExtension::class.java)

/**
 * Convenience method to access the Java plugin extension.
 * @return the [JavaPluginExtension] of the project
 */
val ExtensionContainer.java: JavaPluginExtension
  get() = getByType(JavaPluginExtension::class.java)
