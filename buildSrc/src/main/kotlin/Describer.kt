package org.openstreetmap.josm.gradle.plugin

import java.io.IOException

interface Describer {
  @Throws(IOException::class)
  fun describe(dirty: Boolean = true): String
}
