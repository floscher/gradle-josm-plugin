package org.openstreetmap.josm.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringReader
import java.io.StringWriter

class ReleaseSpecException(override var message: String,
                           override var cause: Throwable?)
  : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}

const val DEFAULT_LATEST_LABEL = "latest"

const val DEFAULT_PICKUP_RELEASE_LABEL = "pickup-release"
const val DEFAULT_PICKUP_RELEASE_DESCRIPTION = """
This is the pickup release for the JOSM plugin system. It
* downloads the plugin jar in this release every 10 minutes
* extracts the metadata from `META-INF/MANIFEST.INF`
* updates the metadata in the
  [JOSM plugin directory](https://josm.openstreetmap.de/plugin)
---
This release currently provides the plugin release
{{ labelForPickedUpRelease }} with the following description:
{{ descriptionForPickedUpRelease }}"""

/**
 * A release specification maintained in the local `releases.yml` file
 */

open class ReleaseSpec(
    /** the release label, i.e. v1.0.0 */
    open val label: String,

    /** the lowest numeric josm version this release is compatible with */
    val numericJosmVersion: Int = 0,

    /** a description for the plugin release */
    open val description: String? = null,

    /** an optional name for the release. Defaults to the label, if missing. */
    val name: String = label
) {
    init {
        assert(numericJosmVersion >= 0)
    }
}

class PickupRelaseSpec(
    override val label: String = DEFAULT_PICKUP_RELEASE_LABEL,
    override val description: String = DEFAULT_PICKUP_RELEASE_DESCRIPTION) :
        ReleaseSpec(label=label, description = description){

    fun descriptionForPickedUpRelease(
        pickedUpReleaseLabel: String,
        pickedUpReleaseDescription: String): String {
        val factory = DefaultMustacheFactory()
        val template = factory.compile(StringReader(description), "description")
        val scope = mapOf(
            "pickedUpReleaseLabel" to pickedUpReleaseLabel,
            "pickedUpReleaseDescription" to pickedUpReleaseDescription
        )
        val writer = StringWriter()
        template.execute(writer, scope)
        return writer.toString()
    }

    fun descriptionForPickedUpRelease(pickedUpRelase: ReleaseSpec): String {
        return descriptionForPickedUpRelease(
            pickedUpRelase.label,
            pickedUpRelase.description ?: "")
    }
}

data class ReleasesSpec(val pickupRelease: PickupRelaseSpec,
                        val releases: List<ReleaseSpec>?) {
    companion object {
        val empty = ReleasesSpec(
            pickupRelease = PickupRelaseSpec(),
            releases = listOf()
        )
        /**
         * Reads the release specifications from the YAML `file`.
         */
        fun load(file: File): ReleasesSpec {
            val mapper = ObjectMapper(YAMLFactory())
            val root = mapper.readTree(file)
            if (root.isNull) return empty
            val pickupReleaseLabel = root.get("pickup_release_for_josm")
                ?.get("label")?.asText()
                ?: DEFAULT_PICKUP_RELEASE_LABEL

            val pickupReleaseDescription = root.get("pickup_release_for_josm")
                    ?.get("description")?.asText()
                ?: DEFAULT_PICKUP_RELEASE_DESCRIPTION

            val pickupRelase =
                PickupRelaseSpec(pickupReleaseLabel, pickupReleaseDescription)

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
                pickupRelease = pickupRelase,
                releases =  releases
            )
        }
    }

    fun hasRelease(label: String) : Boolean =
        this[label] != null

    operator fun get(label: String): ReleaseSpec? =
        releases?.first {it.label == label}

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





