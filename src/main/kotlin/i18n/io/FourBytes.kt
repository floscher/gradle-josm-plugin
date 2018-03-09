package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Container class for four bytes, that can then be converted to a [Long] value, either as big endian or little endian.
 */
class FourBytes(val a: Byte, val b: Byte, val c: Byte, val d: Byte) {
  fun getLongValue(bigEndian: Boolean): Long {
    if (bigEndian) {
      // Big endian: "Beginning at the big end", first byte is most significant
      return FourBytes(d, c, b, a).getLongValue(!bigEndian)
    } else {
      // Little endian: "Beginning at the little end", first byte is least significant
      return (((d.toUnsigned().shl(8) + c.toUnsigned()).shl(8) + b.toUnsigned()).shl(8) + a.toUnsigned())
    }
  }

  /**
   * Convert a signed byte value to an unsigned byte value.
   * @return the value of the unsigned byte (as [Long])
   */
  private fun Byte.toUnsigned(): Long {
    return toLong().and(0xFF)
  }
}
