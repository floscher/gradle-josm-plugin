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
      assertEquals(i / 4, ByteArray(i) { 0 }.toFourByteList().map { it.getUIntValue(true) }.size)
      assertEquals(i / 4, ByteArray(i) { 0 }.toFourByteList().map { it.getUIntValue(false) }.size)
    }

    val bytes0000 = ByteArray(4) { 0.toByte() }
    assertEquals(listOf(0u), bytes0000.toFourByteList().map { it.getUIntValue(true) })
    assertEquals(listOf(0u), bytes0000.toFourByteList().map { it.getUIntValue(false) })

    val BE_1234 = 16909060u
    val LE_1234 = 67305985u
    val bytes1234 = ByteArray(4) { (it + 1).toByte() }
    assertEquals(listOf(BE_1234), bytes1234.toFourByteList().map { it.getUIntValue(true) })
    assertEquals(listOf(LE_1234), bytes1234.toFourByteList().map { it.getUIntValue(false) })

    val BE_FFFEFDFC = 4294901244u
    val LE_FFFEFDFC = 4244504319u
    val bytesFFFEFDFC = ByteArray(4) { (- it - 1).toByte() }
    assertEquals(listOf(BE_FFFEFDFC), bytesFFFEFDFC.toFourByteList().map { it.getUIntValue(true) })
    assertEquals(listOf(LE_FFFEFDFC), bytesFFFEFDFC.toFourByteList().map { it.getUIntValue(false) })

    assertEquals(listOf(0u, BE_1234, BE_FFFEFDFC), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toFourByteList().map { it.getUIntValue(true) })
    assertEquals(listOf(0u, LE_1234, LE_FFFEFDFC), bytes0000.plus(bytes1234).plus(bytesFFFEFDFC).toFourByteList().map { it.getUIntValue(false) })
  }
}
