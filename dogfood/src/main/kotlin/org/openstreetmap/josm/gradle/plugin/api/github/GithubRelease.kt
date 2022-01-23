package org.openstreetmap.josm.gradle.plugin.api.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openstreetmap.josm.gradle.plugin.api.Release

@Serializable
public data class GithubRelease(
  override val name: String?,
  @SerialName("body") override val description: String?,
  @SerialName("tag_name") override val tagName: String,
  @SerialName("published_at") override val releasedAt: String,
  val draft: Boolean,
  val prerelease: Boolean,
  val assets: List<Asset>
): Release() {
  @Serializable
  public data class Asset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long
  )
}
