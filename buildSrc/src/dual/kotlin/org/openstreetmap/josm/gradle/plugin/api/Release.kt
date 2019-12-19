package org.openstreetmap.josm.gradle.plugin.api

import kotlinx.serialization.Transient
import java.time.Instant

abstract class Release {
  abstract val name: String?
  abstract val description: String?
  abstract val tagName: String
  abstract val releasedAt: String?

  @Transient
  val releasedAtEpoch
    get() = releasedAt?.let { Instant.parse(it) }
}
