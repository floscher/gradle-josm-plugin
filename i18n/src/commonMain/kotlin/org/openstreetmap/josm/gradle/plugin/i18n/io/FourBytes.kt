package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Container class for four bytes, that can then be converted to a [Long] value, either as big endian or little endian.
 * @property a first byte
 * @property b second byte
 * @property c third byte
 * @property d fourth byte
 */
public data class FourBytes(val a: Byte, val b: Byte, val c: Byte, val d: Byte) {

  /**
   * @param [big] most significant byte
   * @param [bigish] secondmost significant byte
   * @param [lowish] thirdmost significant byte
   * @param [low] least significant byte
   * @param [bigEndian] `true` if the bytes should come in the order from most significant first
   *  to least significant last, `false` if the order should be exactly in reverse
   */
  public constructor(big: Byte, bigish: Byte, lowish: Byte, low: Byte, bigEndian: Boolean) : this(
    if (bigEndian) big else low,
    if (bigEndian) bigish else lowish,
    if (bigEndian) lowish else bigish,
    if (bigEndian) low else big
  )

  /**
   * Initialize a four byte sequence with a 32 bit unsigned integer, either as big-endian or little-endian.
   * @param uintValue the 32 bit unsigned integer value
   * @param bigEndian if true, the number is encoded in big-endian byte order, otherwise in little-endian
   */
  @ExperimentalUnsignedTypes
  public constructor(uintValue: UInt, bigEndian: Boolean): this(
    uintValue.shr(24).and(0xFFu).toUByte().toByte(),
    uintValue.shr(16).and(0xFFu).toUByte().toByte(),
    uintValue.shr(8).and(0xFFu).toUByte().toByte(),
    uintValue.and(0xFFu).toUByte().toByte(),
    bigEndian
  )

  /**
   * Convert the four (signed) byte values to one unsigned [UInt] value.
   * @param bigEndian determines byte order. If `true`, byte order is big-endian.
   *   Otherwise the byte order is little-endian.
   * @return the [kotlin.UInt] value (unsigned 32 bit) represented by the four bytes [a], [b], [c] and [d], respecting the given byte order.
   */
  @ExperimentalUnsignedTypes
  public fun getUIntValue(bigEndian: Boolean): UInt =
    if (bigEndian) {
      // Big endian: "Beginning at the big end", first byte is most significant
      FourBytes(d, c, b, a).getUIntValue(!bigEndian)
    } else {
      // Little endian: "Beginning at the little end", first byte is least significant
      (
        (
          d.toUByte().toUInt().shl(8) + c.toUByte().toUInt()
        ).shl(8) + b.toUByte().toUInt()
      ).shl(8) + a.toUByte().toUInt()
    }
}
