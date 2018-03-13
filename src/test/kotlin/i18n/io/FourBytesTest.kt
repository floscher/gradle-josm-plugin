package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.pow

class FourBytesTest {
  // Long value of 0x0000FFFF (unsigned)
  val longFFFF = 16.0.pow(4).toLong() - 1
  // Long value of 0xFFFFFFFF (unsigned)
  val longFFFFFFFF = 16.0.pow(8).toLong() - 1
  // Long value of 0x12345678 (unsigned)
  val long12345678 = (0x12 shl 8 xor 0x34 shl 8 xor 0x56 shl 8 xor 0x78).toLong()
  // Long value of 0x78563412 (unsigned)
  val long78563412 = (0x78 shl 8 xor 0x56 shl 8 xor 0x34 shl 8 xor 0x12).toLong()

  @Test
  fun testConversion() {
    val bytes00000000 = FourBytes(0,0,0, 0)
    val bytesFFFFFFFF = FourBytes(-1, -1, -1, -1)
    val bytesFFFF0000 = FourBytes(-1, -1, 0, 0)
    val bytes12345678 = FourBytes(18, 52, 86, 120)

    assertEquals(0L, bytes00000000.getLongValue(bigEndian = true))
    assertEquals(0L, bytes00000000.getLongValue(bigEndian = false))

    assertEquals(longFFFFFFFF, bytesFFFFFFFF.getLongValue(bigEndian = true))
    assertEquals(longFFFFFFFF, bytesFFFFFFFF.getLongValue(bigEndian = false))


    assertEquals(longFFFF, bytesFFFF0000.getLongValue(bigEndian = false))
    assertEquals(longFFFFFFFF - longFFFF, bytesFFFF0000.getLongValue(bigEndian = true))

    assertEquals(long12345678, bytes12345678.getLongValue(bigEndian = true))
    assertEquals(long78563412, bytes12345678.getLongValue(bigEndian = false))
  }
}
