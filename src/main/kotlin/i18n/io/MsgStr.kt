package org.openstreetmap.josm.gradle.plugin.i18n.io

data class MsgStr(val strings: List<String>) {
  val singularString = strings[0]
  val numPlurals = strings.size - 1
}
