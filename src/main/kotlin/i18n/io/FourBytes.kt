package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Container class for four bytes, that can then be converted to a [Long] value, either as big endian or little endian.
 * @param a first byte
 * @param b second byte
 * @param c third byte
 * @param d fourth byte
 */
class FourBytes(val a: Byte, val b: Byte, val c: Byte, val d: Byte) {

  /**
   * Convert the four byte values to one long value.
   * @param bigEndian determines byte order. If `true`, byte order is big-endian.
   *   Otherwise the byte order is little-endian.
   * @return the long value represented by the four bytes [a], [b], [c] and [d], respecting the given byte order.
   */
  fun getLongValue(bigEndian: Boolean): Long =
    if (bigEndian) {
      // Big endian: "Beginning at the big end", first byte is most significant
      FourBytes(d, c, b, a).getLongValue(!bigEndian)
    } else {
      // Little endian: "Beginning at the little end", first byte is least significant
      (((d.toUnsigned().shl(8) + c.toUnsigned()).shl(8) + b.toUnsigned()).shl(8) + a.toUnsigned())
    }

  /**
   * Convert a signed byte value to an unsigned byte value.
   * @return the value of the unsigned byte (as [Long])
   */
  private fun Byte.toUnsigned() = toLong().and(0xFF)
}
