package org.openstreetmap.josm.gradle.plugin.i18n.io

data class MsgStr(val strings: List<String>) {
  constructor(firstString: String, vararg moreStrings: String): this(listOf(firstString, *moreStrings))
  init {
    require(strings.size >= 1){"A MsgStr has to consist of at least one string!"}
  }
}
