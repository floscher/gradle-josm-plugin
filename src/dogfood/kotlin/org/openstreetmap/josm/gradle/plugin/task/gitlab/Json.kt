package org.openstreetmap.josm.gradle.plugin.task.gitlab

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.JsonConfiguration

@UnstableDefault
private val nonstrictBacker = JsonConfiguration(
  isLenient = true,
  ignoreUnknownKeys = true,
  serializeSpecialFloatingPointValues = true,
  useArrayPolymorphism = true
)

@UnstableDefault
val JsonConfiguration.Companion.Nonstrict
  get() = nonstrictBacker
