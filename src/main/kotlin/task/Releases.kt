package org.openstreetmap.josm.gradle.plugin.task

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.nio.file.Files

/**
 * A release specification maintained in the local `releases.yml` file
 */
data class ReleaseSpec(
  @JsonPropertyDescription("the release label, i.e. v1.0.0")
  val label: String,

  @JsonPropertyDescription("the numeric plugin version, monotonically increasing positive integer")
  @JsonProperty("numeric_plugin_version")
  val numericPluginVersion: Int,

  @JsonPropertyDescription("the lowest numeric josm version this release is compatible with")
  @JsonProperty("numeric_josm_version")
  val numericJosmVersion: Int,

  @JsonPropertyDescription("a description for the plugin release")
  val description: String? = null,

  @JsonPropertyDescription("an optional name for the release. Defaults to the label, if missing.")
  val name: String? = null
)

/**
 * A list of release specifications
 */
data class Releases(val releases: List<ReleaseSpec>?) {
    companion object {
        /**
         * Reads the release specifications from the YAML `file`.
         */
        fun load(file: File): Releases {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())
            val releases= Files.newBufferedReader(file.toPath()).use {
                mapper.readValue(it, Releases::class.java).releases ?: listOf()
            }
            return Releases(releases)
        }
    }

    fun hasRelease(label: String) : Boolean = releases?.find {
        it.label == label } != null

    fun hasRelease(numericPluginVersion: Int) : Boolean = releases?.find {
        it.numericPluginVersion == numericPluginVersion} != null

}





