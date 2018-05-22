package org.openstreetmap.josm.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class ReleaseSpecException(override var message: String,
                           override var cause: Throwable?)
  : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}

const val DEFAULT_LATEST_NAME = "latest"

/**
 * A release specification maintained in the local `releases.yml` file
 */
data class ReleaseSpec(
  /** the release label, i.e. v1.0.0 */
  val label: String,

  /** the numeric plugin version, monotonically increasing positive integer */
  val numericPluginVersion: Int,

  /** the lowest numeric josm version this release is compatible with */
  val numericJosmVersion: Int,

  /** a description for the plugin release */
  val description: String? = null,

  /** an optional name for the release. Defaults to the label, if missing. */
  val name: String? = null
)

data class ReleasesSpec(val latest: String, val releases: List<ReleaseSpec>?) {
    companion object {
        /**
         * Reads the release specifications from the YAML `file`.
         */
        fun load(file: File): ReleasesSpec? {
            val mapper = ObjectMapper(YAMLFactory())
            val root = mapper.readTree(file)
            if (root.isNull) return null
            val latest = root.get("latest_release")?.get("name")?.asText()
                ?: DEFAULT_LATEST_NAME
            val releases = root.get("releases")?.mapIndexed {i, release ->
                if (release.isNull) return null
                val label = release.get("label")?.asText() ?:
                    throw ReleaseSpecException(
                      """Missing label for release with index $i
                      | releases:
                      |   ....
                      |   - label: ....
                      |       ^ ... missing?
                      """.trimMargin("|")
                    )
                val numericJosmVersion =
                    release.get("numeric_josm_version")?.let {
                    try {
                        it.asText().toInt()
                    } catch(e: NumberFormatException) {
                        throw ReleaseSpecException(
                            """Illegal numeric_josm_version for release
                            | specification with label '$label'
                             """.trimMargin("|"), e)
                    }
                } ?: 0

                val numericPluginVersion =
                    release.get("numeric_plugin_version")?.let {
                      try {
                        it.asText().toInt()
                      } catch(e: NumberFormatException) {
                        throw ReleaseSpecException("""Illegal numeric_plugin_version for release
                            | specification with label '$label'
                             """.trimMargin("|"), e)
                      }
                } ?: 0

                ReleaseSpec(
                    label = label,
                    numericJosmVersion = numericJosmVersion,
                    numericPluginVersion = numericPluginVersion,
                    description = release.get("description")?.asText(),
                    name = release.get("name")?.asText() ?: label
                )
             }
            return ReleasesSpec(
                latest = latest,
                releases =  releases
            )
        }
    }

    fun hasRelease(label: String) : Boolean = releases?.find {
        it.label == label } != null

    fun hasRelease(numericPluginVersion: Int) : Boolean = releases?.find {
        it.numericPluginVersion == numericPluginVersion} != null
}


/**
 * A list of release specifications
 */
private data class Releases(val releases: List<ReleaseSpec>?) {

}





