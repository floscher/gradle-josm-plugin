package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.IOException
import java.io.InputStream

/**
 * Read bytes from the input stream into the array. If there are not enough
 * bytes to fill the array, an exception is thrown.
 * @throws IOException if reading from the [InputStream] fails (see [InputStream.read]) or if the number of bytes that
 *   can be read is lower than the length of the parameter [b].
 */
@Throws(IOException::class)
public fun InputStream.readAllOrException(b: ByteArray): Int =
  if (b.isEmpty()) {
    0
  } else {
    this.read(b).also { // TODO: Handle the case where multiple invocations of read() are needed
      if (it != b.size) {
        throw IOException("Could not read ${b.size} bytes. The stream ended unexpectedly!")
      }
    }
  }

/**
 * Skip over `n` bytes. If there are only less than `n` bytes to skip over, an exception is thrown.
 * @param n the number of bytes to skip
 */
@ExperimentalUnsignedTypes
@Throws(IOException::class)
public fun InputStream.skipAllOrException(n: UInt): UInt {
  val numBytes = this.skip(n.toLong())
  if (numBytes < n.toLong()) {
    throw IOException("Could not skip over $n bytes. The stream ended unexpectedly!")
  }
  return numBytes.toUInt()
}

/**
 * Read the next two bytes from the [InputStream], interpret them as a big-endian number (first byte most significant).
 * This number is then returned.
 * @return -1 if no bytes can be read, otherwise the number that
 * @throws IOException if only one byte can be read from the [InputStream]
 */
@Throws(IOException::class)
public fun InputStream.readTwoBytesAsInt(): Int {
  val bytes = Pair(read(), read())
  if (bytes.first < 0) {
    return -1
  }
  if (bytes.second < 0) {
    throw IOException("Stream ended unexpectedly!")
  }
  return bytes.first.shl(8) + bytes.second
}
