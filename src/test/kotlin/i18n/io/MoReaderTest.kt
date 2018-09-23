package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoReaderTest {
  @Test
  fun testByteArrayToMsgId() {
    val singularString = "SingularStringÄß"
    val pluralString1 = "PluralString1Äß"
    val pluralString2 = "PluralString2Äß"
    val context = "someContext"

    assertEquals(MsgId(MsgStr(singularString)), singularString.toByteArray().toMsgId())
    assertEquals(MsgId(MsgStr(singularString, pluralString1, pluralString2)), "$singularString\u0000$pluralString1\u0000$pluralString2".toByteArray().toMsgId())

    assertEquals(MsgId(MsgStr(singularString), context), "$context\u0004$singularString".toByteArray().toMsgId())
    assertEquals(MsgId(MsgStr(singularString, pluralString1), context), "$context\u0004$singularString\u0000$pluralString1".toByteArray().toMsgId())
  }

  @Test
  fun testByteListToLong() {
    for (i in 0..20) {
      assertEquals(i / 4, ByteArray(i) { 0 }.toList().toLongList(true).size)
      assertEquals(i / 4, ByteArray(i) { 0 }.toList().toLongList(false).size)
    }

    val bytes0000 = listOf(0.toByte(), 0.toByte(), 0.toByte(),  0.toByte())
    assertEquals(listOf(0.toLong()), bytes0000.toLongList(true))
    assertEquals(listOf(0.toLong()), bytes0000.toLongList(false))

    val bytes1234 = listOf(1.toByte(), 2.toByte(), 3.toByte(),  4.toByte())
    assertEquals(listOf<Long>(16909060), bytes1234.toLongList(true))
    assertEquals(listOf<Long>(67305985), bytes1234.toLongList(false))

    val bytesFFFEFDFC = listOf((-1).toByte(), (-2).toByte(), (-3).toByte(), (-4).toByte())
    assertEquals(listOf(4294901244), bytesFFFEFDFC.toLongList(true))
    assertEquals(listOf(4244504319), bytesFFFEFDFC.toLongList(false))

    assertEquals(listOf(0, 16909060, 4294901244), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toLongList(true))
    assertEquals(listOf(0, 67305985, 4244504319), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toLongList(false))
  }
}
