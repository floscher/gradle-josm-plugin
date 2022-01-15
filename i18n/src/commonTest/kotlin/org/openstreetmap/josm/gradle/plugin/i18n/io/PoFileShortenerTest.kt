package org.openstreetmap.josm.gradle.plugin.i18n.io

import kotlin.test.Test
import kotlin.test.assertEquals

const val ORIGINAL_PO_FILE_CONTENT =
"""# SOME DESCRIPTIVE TITLE.
# Copyright (C) 2021 THE PACKAGE'S COPYRIGHT HOLDER
# This file is distributed under the same license as the josm-plugin_test package.
# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.${" \t" /* A bit of trailing whitespace in the header */}
#
# Translators:
# John Doe <john@example.org>, 2020
# Max Mustermann, 2021
#
#, fuzzy
msgid ""
msgstr ""
"Project-Id-Version: josm-plugin_test 1.2.3\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 1970-01-01 00:00+0000\n"
"PO-Revision-Date: 2021-01-01 00:00+0000\n"
"Last-Translator: Max Mustermann, 2021\n"
"Language-Team: German (https://example.org/team/42)\n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=UTF-8\n"
"Content-Transfer-Encoding: 8bit\n"
"Language: de\n"
"Plural-Forms: nplurals=2; plural=(n != 1);\n"

#: example.org/file/Example.java#L42
msgid "Example"
msgstr "Beispiel"

#: example.org/file/Example.java#L1729
#, java-format
msgid "{0} example"
msgid_plural "{0} examples"${" \t" /* A bit of trailing whitespace in the body */}
msgstr[0] "{0} Beispiel"
msgstr[1] "{0} Beispiele"

"""

const val SHORTENED_PO_FILE_CONTENT =
"""# Title for the project (de)
# Copyright (C) 2021 Jane Doe
# This file is distributed under the same license as the josm-plugin_test package.
# FIRST AUTHOR, YEAR.
#
# Translators:
# John Doe, 2020
# Max Mustermann, 2021
#
msgid ""
msgstr ""
"Project-Id-Version: josm-plugin_test 1.2.3\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 1970-01-01 00:00+0000\n"
"PO-Revision-Date: 2021-01-01 00:00+0000\n"
"Language-Team: German (https://example.org/team/42)\n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=UTF-8\n"
"Content-Transfer-Encoding: 8bit\n"
"Language: de\n"
"Plural-Forms: nplurals=2; plural=(n != 1);\n"

msgid "Example"
msgstr "Beispiel"

msgid "{0} example"
msgid_plural "{0} examples"
msgstr[0] "{0} Beispiel"
msgstr[1] "{0} Beispiele"
"""

const val TITLE = "Title for the project (de)"
const val COPYRIGHT = "Jane Doe"
const val PACKAGE = "josm-plugin_example"

class PoFileShortenerTest {
  @Test
  fun test() {
    assertEquals(
      SHORTENED_PO_FILE_CONTENT,
      ORIGINAL_PO_FILE_CONTENT.split('\n').shortenPoFile(TITLE, COPYRIGHT, PACKAGE)
    )
  }

  @Test
  fun testEmpty() {
    assertEquals(
      "\n",
      listOf<String>().shortenPoFile(TITLE, COPYRIGHT, PACKAGE)
    )
    assertEquals(
      "#\n",
      listOf("#").shortenPoFile(TITLE, COPYRIGHT, PACKAGE)
    )
    assertEquals(
      "# $TITLE\n",
      listOf("# SOME DESCRIPTIVE TITLE.").shortenPoFile(TITLE, COPYRIGHT, PACKAGE)
    )
  }
}
