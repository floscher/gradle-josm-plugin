package org.openstreetmap.josm.gradle.plugin.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.InputStream

/**
 * A release specification maintained in the local `releases.yml` file
 * @param [label] the release label, e.g. 1.2.3
 * @param [minJosmVersion] the lowest JOSM version this release is compatible with
 * @param [description] a description for the plugin release (optional)
 * @param [name] a name for the release. If omitted, null or blank, it's set to "Release [label]".
 */

open class ReleaseSpec(
  val label: String,
  val minJosmVersion: Int,
  val description: String? = null,
  name: String? = null
) {
  init {
    require(label.isNotBlank())
    require(minJosmVersion >= 0)
  }

  /**
   * An optional name for the release. Used as a "headline" for the description.
   * Defaults to "Release [label]", if missing.
   */
  val name = name?.takeIf { it.isNotBlank() } ?: "Release $label"

  companion object {
    private const val KEY_RELEASES = "releases"
    private const val KEY_RELEASE_LABEL = "label"
    private const val KEY_RELEASE_MIN_JOSM_VERSION = "minJosmVersion"
    private const val KEY_RELEASE_DESCRIPTION = "description"
    private const val KEY_RELEASE_NAME = "name"

    fun loadListFrom(stream: InputStream): List<ReleaseSpec> =
      ObjectMapper(YAMLFactory()).readTree(stream)
        .takeIf { !it.isNull } // null, if root node has null type
        ?.get(KEY_RELEASES) // get releases node
        ?.mapIndexedNotNull { i, release ->
          val labelNode = release[KEY_RELEASE_LABEL]
          val label = labelNode?.asText()?.takeIf { it.isNotBlank() }
            ?: throw GithubReleaseException(
              "Release with index $i has " +
                if (labelNode == null) {
                  "missing '$KEY_RELEASE_LABEL:'!"
                } else {
                  "'$KEY_RELEASE_LABEL:' of wrong type (${labelNode.nodeType} instead of STRING)!"
                }
            )

          val minJosmVersionNode = release[KEY_RELEASE_MIN_JOSM_VERSION]
          val minJosmVersion = minJosmVersionNode?.let {
            if (it.isInt) it.asInt() else null
          } ?: throw GithubReleaseException(
            "Release with label '$label' has " +
              if (minJosmVersionNode == null) {
                "missing '$KEY_RELEASE_MIN_JOSM_VERSION:'!"
              } else {
                "'$KEY_RELEASE_MIN_JOSM_VERSION:' of wrong type (${minJosmVersionNode.nodeType} instead of INTEGER)"
              }
          )

          val description = release[KEY_RELEASE_DESCRIPTION]?.let {
            if (it.isTextual) it.asText() else null
          }

          val name = release[KEY_RELEASE_NAME]?.let {
            if (it.isTextual) it.asText() else null
          }

          ReleaseSpec(label, minJosmVersion, description, name)
        } ?: listOf()
  }
}

/**
 * Replies a list of only the relevant releases whose download URLs
 * have to be included in  the `MANIFEST` file of a plugin jar.
 * For each of the resulting releases, it is true that there is no
 * other release with the same `minJosmVersion` that is newer.
 */
fun List<ReleaseSpec>.onlyFallbackVersions() =
  this.asSequence()
    .groupBy { it.minJosmVersion }
    .map { it.value.last() }

operator fun List<ReleaseSpec>.get(label: String) = find { it.label == label }
