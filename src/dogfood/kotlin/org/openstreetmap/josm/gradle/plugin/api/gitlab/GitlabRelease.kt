package org.openstreetmap.josm.gradle.plugin.api.gitlab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openstreetmap.josm.gradle.plugin.api.Release

@Serializable
data class GitlabRelease<T: GitlabRelease.Assets.Link>(
  override val name: String,
  @SerialName("tag_name") override val tagName: String,
  override val description: String,
  val assets: Assets<T>,
  @SerialName("released_at") override val releasedAt: String? = null
): Release() {

  @Serializable
  data class Assets<T: Assets.Link>(val links: List<T>) {

    sealed class Link() {
      abstract val name: String
      abstract val url: String

      @Serializable
      data class Existing(
        override val name: String,
        override val url: String,
        val external: Boolean
      ): Link()

      @Serializable
      data class New(
        override val name: String,
        override val url: String
      ): Link()
    }
  }
}
