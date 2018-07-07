package org.openstreetmap.josm.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class ReleaseSpecException(override var message: String,
                           override var cause: Throwable?)
  : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}

const val DEFAULT_LATEST_LABEL = "latest"

/**
 * A release specification maintained in the local `releases.yml` file
 */
data class ReleaseSpec(
    /** the release label, i.e. v1.0.0 */
    val label: String,

    /** the lowest numeric josm version this release is compatible with */
    val numericJosmVersion: Int,

    /** a description for the plugin release */
    val description: String? = null,

    /** an optional name for the release. Defaults to the label, if missing. */
    val name: String = label
) {
    init {
        assert(numericJosmVersion >= 0)
    }
}

data class ReleasesSpec(val latestLabel: String, val releases: List<ReleaseSpec>?) {
    companion object {
        val empty = ReleasesSpec(
            latestLabel = DEFAULT_LATEST_LABEL,
            releases = listOf()
        )
        /**
         * Reads the release specifications from the YAML `file`.
         */
        fun load(file: File): ReleasesSpec {
            val mapper = ObjectMapper(YAMLFactory())
            val root = mapper.readTree(file)
            if (root.isNull) return empty
            val latestLabel = root.get("latest_release")?.get("label")?.asText()
                ?: DEFAULT_LATEST_LABEL
            val releases = root.get("releases")
                ?.mapIndexedNotNull {i, release ->
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
                } ?: throw ReleaseSpecException(
                    """Missing numeric_josm_version for release
                    | specification with label '$label'"""
                        .trimMargin("|"))

                ReleaseSpec(
                    label = label,
                    numericJosmVersion = numericJosmVersion,
                    description = release.get("description")?.asText(),
                    name = release.get("name")?.asText() ?: label
                )
             }
            return ReleasesSpec(
                latestLabel = latestLabel,
                releases =  releases
            )
        }
    }

    fun hasRelease(label: String) : Boolean =
        this[label] != null

    operator fun get(label: String): ReleaseSpec? =
        releases?.first {it.label == label}

    val latestRelease: ReleaseSpec by lazy {
        ReleaseSpec(
            label = this.latestLabel,
            numericJosmVersion = 0
        )
    }

    /**
     * Replies the list of numeric JOSM versions for which
     * releases are configured
     */
    fun josmVersions(): List<Int> =
        releases?.map { it.numericJosmVersion }
            ?.sorted()?.distinct()
            ?: listOf()

    /**
     * Replies the list of relevant releases whose download URLs
     * have to be included in  the `MANIFEST` file of a plugin
     * jar.
     */
    fun relevantReleasesForDownloadUrls(): List<ReleaseSpec> =
        josmVersions().map { v ->
            releases?.first { it.numericJosmVersion == v }
        }.filterNotNull()
}





