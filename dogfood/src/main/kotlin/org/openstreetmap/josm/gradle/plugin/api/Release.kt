package org.openstreetmap.josm.gradle.plugin.api

import kotlinx.serialization.Transient
import java.time.Instant

public abstract class Release {
  public abstract val name: String?
  public abstract val description: String?
  public abstract val tagName: String
  public abstract val releasedAt: String?

  @Transient
  public val releasedAtEpoch: Instant?
    get() = releasedAt?.let { Instant.parse(it) }
}
