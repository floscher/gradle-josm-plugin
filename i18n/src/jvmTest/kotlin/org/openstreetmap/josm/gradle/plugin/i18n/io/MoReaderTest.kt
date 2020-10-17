package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class MoReaderTest {
  @Test
  fun testByteArrayToMsgId() {
    val singularString = "SingularStringÄß"
    val pluralString1 = "PluralString1Äß"
    val pluralString2 = "PluralString2Äß"
    val context = "someContext"

    assertEquals(MsgId(MsgStr(singularString)), singularString.toByteArray().toMsgId())
    assertEquals(MsgId(MsgStr(singularString, pluralString1, pluralString2)), "$singularString${MsgStr.GRAMMATICAL_NUMBER_SEPARATOR}$pluralString1${MsgStr.GRAMMATICAL_NUMBER_SEPARATOR}$pluralString2".toByteArray().toMsgId())

    assertEquals(MsgId(MsgStr(singularString), context), "$context${MsgId.CONTEXT_SEPARATOR}$singularString".toByteArray().toMsgId())
    assertEquals(MsgId(MsgStr(singularString, pluralString1), context), "$context${MsgId.CONTEXT_SEPARATOR}$singularString${MsgStr.GRAMMATICAL_NUMBER_SEPARATOR}$pluralString1".toByteArray().toMsgId())
  }

  @Test
  fun testByteListToLong() {
    for (i in 0..20) {
      assertEquals(i / 4, ByteArray(i) { 0 }.toUIntList(true).size)
      assertEquals(i / 4, ByteArray(i) { 0 }.toUIntList(false).size)
    }

    val bytes0000 = ByteArray(4) { 0.toByte() }
    assertEquals(listOf(0u), bytes0000.toUIntList(true))
    assertEquals(listOf(0u), bytes0000.toUIntList(false))

    val bytes1234 = ByteArray(4) { (it + 1).toByte() }
    assertEquals(listOf(16909060u), bytes1234.toUIntList(true))
    assertEquals(listOf(67305985u), bytes1234.toUIntList(false))

    val bytesFFFEFDFC = ByteArray(4) { (- it - 1).toByte() }
    assertEquals(listOf(4294901244u), bytesFFFEFDFC.toUIntList(true))
    assertEquals(listOf(4244504319u), bytesFFFEFDFC.toUIntList(false))

    assertEquals(listOf(0u, 16909060u, 4294901244u), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toUIntList(true))
    assertEquals(listOf(0u, 67305985u, 4244504319u), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toUIntList(false))
  }
}
