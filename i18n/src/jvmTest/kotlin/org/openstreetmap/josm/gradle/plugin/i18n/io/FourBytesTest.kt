package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class FourBytesTest {
  // Long value of 0x0000FFFF (unsigned)
  val uint0000FFFF = 0xFFFF_u
  // Long value of 0xFFFFFFFF (unsigned)
  val uintFFFFFFFF = 0xFFFFFFFF_u
  // Long value of 0x12345678 (unsigned)
  val uint12345678 = (0x12 shl 8 xor 0x34 shl 8 xor 0x56 shl 8 xor 0x78).toUInt()
  // Long value of 0x78563412 (unsigned)
  val uint78563412 = (0x78 shl 8 xor 0x56 shl 8 xor 0x34 shl 8 xor 0x12).toUInt()

  val bytes00000000 = FourBytes(0,0,0, 0)
  val bytesFFFFFFFF = FourBytes(-1, -1, -1, -1)
  val bytesFFFF0000 = FourBytes(-1, -1, 0, 0)
  val bytes0000FFFF = FourBytes(0, 0, -1, -1)
  val bytes12345678 = FourBytes(18, 52, 86, 120)
  val bytes78563412 = FourBytes(120, 86, 52, 18)

  @Test
  fun testConversion() {
    assertEquals(0u, bytes00000000.getUIntValue(bigEndian = true))
    assertEquals(0u, bytes00000000.getUIntValue(bigEndian = false))

    assertEquals(uintFFFFFFFF, bytesFFFFFFFF.getUIntValue(bigEndian = true))
    assertEquals(uintFFFFFFFF, bytesFFFFFFFF.getUIntValue(bigEndian = false))

    assertEquals(uint0000FFFF, bytesFFFF0000.getUIntValue(bigEndian = false))
    assertEquals(uintFFFFFFFF - uint0000FFFF, bytesFFFF0000.getUIntValue(bigEndian = true))

    assertEquals(uint12345678, bytes12345678.getUIntValue(bigEndian = true))
    assertEquals(uint78563412, bytes12345678.getUIntValue(bigEndian = false))
  }

  @Test
  fun testConversion2() {
    assertEquals(bytes0000FFFF, FourBytes(uint0000FFFF, bigEndian = true))
    assertEquals(bytesFFFF0000, FourBytes(uint0000FFFF, bigEndian = false))
    assertEquals(bytes12345678, FourBytes(uint12345678, bigEndian = true))
    assertEquals(bytes78563412, FourBytes(uint12345678, bigEndian = false))
    assertEquals(bytes78563412, FourBytes(uint78563412, bigEndian = true))
    assertEquals(bytes12345678, FourBytes(uint78563412, bigEndian = false))
  }
}
