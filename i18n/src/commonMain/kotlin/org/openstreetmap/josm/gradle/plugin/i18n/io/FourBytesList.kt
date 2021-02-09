package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Converts a list of [FourBytes] values to one single [ByteArray].
 * @return the [ByteArray] representation of the [FourBytes] list
 */
public fun List<FourBytes>.toBytes(): List<Byte> = (0 until size * 4).map { i ->
  this[i / 4].let {
    when (i % 4) {
      0 -> it.a
      1 -> it.b
      2 -> it.c
      else -> it.d
    }
  }
}

/**
 * Converts a list of bytes to a list of [FourBytes] values.
 *
 * Starting with the first [Byte] in the array, each group of four consecutive bytes are combined into
 * one [FourBytes] value. The resulting list is returned.
 *
 * See [FourBytes] for details on how the byte values are combined.
 *
 * @throws IllegalArgumentException If the size of the [ByteArray] is not a multiple of 4.
 */
@Throws(IllegalArgumentException::class)
public fun List<Byte>.toFourByteList(): List<FourBytes> {
  require(size % 4 == 0) {
    "Expected byte list to have a length that is divisible by 4 (length is $size)!"
  }
  return (0 until size step 4).map {
    FourBytes(get(it), get(it + 1), get(it + 2), get(it + 3))
  }
}
