package org.openstreetmap.josm.gradle.plugin.i18n.io

import kotlin.test.Test
import kotlin.test.assertEquals

class MsgIdTest {
  @Test
  fun testComparable() {
    val baseList = listOf(
      MsgId(MsgStr("A")), MsgId(MsgStr("B")), MsgId(MsgStr("C")),
      MsgId(MsgStr("B"), "a"), MsgId(MsgStr("B"), "b"), MsgId(MsgStr("B"), "c"),
      MsgId(MsgStr("A", "B")), MsgId(MsgStr("A", "A")), MsgId(MsgStr("B", "C")),
      MsgId(MsgStr("A")), MsgId(MsgStr("C"), "b"),
    )

    assertEquals(
      listOf(
        MsgId(MsgStr("A")),
        MsgId(MsgStr("A")),
        MsgId(MsgStr("B")),
        MsgId(MsgStr("C")),
        MsgId(MsgStr("B"), "a"),
        MsgId(MsgStr("B"), "b"),
        MsgId(MsgStr("C"), "b"),
        MsgId(MsgStr("B"), "c"),
        MsgId(MsgStr("A", "A")),
        MsgId(MsgStr("A", "B")),
        MsgId(MsgStr("B", "C")),
      ),
      baseList.sorted()
    )

    assertEquals(
      listOf(
        MsgId(MsgStr("B", "C")),
        MsgId(MsgStr("A", "B")),
        MsgId(MsgStr("A", "A")),
        MsgId(MsgStr("B"), "c"),
        MsgId(MsgStr("C"), "b"),
        MsgId(MsgStr("B"), "b"),
        MsgId(MsgStr("B"), "a"),
        MsgId(MsgStr("C")),
        MsgId(MsgStr("B")),
        MsgId(MsgStr("A")),
        MsgId(MsgStr("A")),
      ),
      baseList.sortedDescending()
    )
  }
}
