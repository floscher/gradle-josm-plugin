package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Takes a list of all the lines in a *.po file.
 * Some default placeholders in the header are replaced and comments referencing the source file locations are removed.
 * @return the new content of the shortened *.po file
 */
public fun List<String>.shortenPoFile(title: String, copyrightHolder: String, packageName: String): String =
  // find first non-header line
  indexOfFirst { !it.startsWith("# ") && it != "#" }
    // split into header lines and non-header lines
    .let {
      if (it < 0) {
        this to emptyList() // file consists of only header lines
      } else {
        subList(0, it) to subList(it, size)
      }
    }
    // Replace the placeholders in the header and filter the non-header lines
    .let { (headerLines: List<String>, bodyLines: List<String>) ->
      headerLines.map {
        it.replace("SOME DESCRIPTIVE TITLE.", title)
          .replace("THE PACKAGE'S COPYRIGHT HOLDER", copyrightHolder)
          .replace("PACKAGE package", "$packageName package")
          .replace(Regex(" ?<[^@]+@[^>]+>"), "")
      }.plus(
        // add all non-header lines except the ones with the source location and the
        bodyLines.filter { !it.startsWith("#, ") && !it.startsWith("#: ") }
      )
    }
    // trim all trailing whitespace and join all lines together
    .joinToString(separator = "\n") { it.trimEnd() }.trimEnd()
    // Remove the "Last-Translator" line in the translation of the empty string
    .replace(("(msgid \"\"\nmsgstr \"\"\n(\"[^\n]+\n)*)\"Last-Translator: [^\n]+\n").toRegex(), "$1") +
    // add final newline
    '\n'
