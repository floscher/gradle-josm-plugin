package org.openstreetmap.josm.gradle.plugin.task.gitlab

import kotlinx.serialization.json.Json

internal val nonstrict = Json {
  isLenient = true
  ignoreUnknownKeys = true
  allowSpecialFloatingPointValues = true
  useArrayPolymorphism = true
}
