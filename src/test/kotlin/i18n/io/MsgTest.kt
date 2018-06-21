package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MsgTest {
  @Test
  fun testMsgId() {
    val arbitraryID = "some arbitrary ID ä—\uD83D\uDC4D\uD83C\uDFFD"
    val arbitraryContext = "some arbitrary context ä—\uD83D\uDC4D\uD83C\uDFFD"
    val id = MsgId(MsgStr(arbitraryID), arbitraryContext)
    assertEquals(listOf(arbitraryID), id.id.strings)
    assertEquals(arbitraryContext, id.context)

    val pluralizedIDs = listOf("ID1", "ID2", "ID3")
    val idNullContext = MsgId(MsgStr(pluralizedIDs), null)
    assertEquals(pluralizedIDs, idNullContext.id.strings)
    assertEquals(null, idNullContext.context)
  }

  @Test
  fun testMsgStr() {
    assertThrows(IllegalArgumentException::class.java) { MsgStr(listOf()) }
    val alphabetList = listOf("A", "B", "C", "D", "E")
    assertEquals(alphabetList, MsgStr("A", "B", "C", "D", "E").strings)
    assertEquals(alphabetList, MsgStr(alphabetList).strings)
  }
}

