package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

class MoReader(val moFileURL: URL) {
  companion object {
    /**
     * The little-endian magic bytes (big-endian would be reversed)
     */
    val MAGIC: List<Byte> = listOf(/* 0x95 */ -107, /* 0x04 */ 4, /* 0x12 */ 18, /* 0xde */ -34)
  }

  var bigEndian: Boolean = true
    private set
  var formatRev: Long = 0
    private set
  var numStrings: Int = 0
    private set
  var offsetOrigStrings: Long = 0
    private set
  var offsetTranslatedStrings: Long = 0
    private set
  var sizeHashingTable: Long = 0
    private set
  var offsetHashingTable: Long = 0
    private set

  fun readFile(): Map<MsgId, MsgStr> {
    // Stream 1 reads the indices of the strings
    val stream1 = moFileURL.openStream()
    // Stream 2 reads the actual strings
    val stream2 = moFileURL.openStream()
    val stringMap: MutableMap<MsgId, MsgStr> = mutableMapOf()
    stream1.use { s1 ->
      // Read the header (sets the header fields)
      var stream1Pos: Long = readHeader(s1)
      stream2.use { s2 ->
        var stream2Pos: Long = 0

        stream1Pos += safeSkip(s1, offsetOrigStrings - stream1Pos)

        // Read msgid strings
        var stringBytes: ByteArray
        val stringLengthOffset = ByteArray(8, { 0 })
        var stringDescriptor: List<Long>
        val msgIds: MutableList<MsgId> = mutableListOf()
        for (i in 0 until numStrings) {
          stream1Pos += safeRead(s1, stringLengthOffset)
          stringDescriptor = stringLengthOffset.toList().toLongList(bigEndian)
          if (stringDescriptor[0] > Int.MAX_VALUE) {
            throw NotImplementedError("Strings longer than ${Int.MAX_VALUE} can not be read! You are trying to read one of length ${stringDescriptor[0]}")
          }

          stream2Pos += safeSkip(s2, stringDescriptor[1] - stream2Pos)
          stringBytes = ByteArray(stringDescriptor[0].toInt(), {0})
          stream2Pos += safeRead(s2, stringBytes)

          msgIds.add(stringBytes.toMsgId())
        }

        stream1Pos += safeSkip(s1, offsetTranslatedStrings - stream1Pos)

        // Read msgstr strings
        for (i in 0 until numStrings) {
          stream1Pos += safeRead(s1, stringLengthOffset)
          stringDescriptor = stringLengthOffset.toList().toLongList(bigEndian)

          stream2Pos += safeSkip(s2, stringDescriptor[1] - stream2Pos)
          stringBytes = ByteArray(stringDescriptor[0].toInt(), {0})
          stream2Pos += safeRead(s2, stringBytes)

          stringMap[msgIds.get(i)] = MsgStr(String(stringBytes, StandardCharsets.UTF_8).split('\u0000'))
        }
      }
    }
    return stringMap.toMap()
  }

  private fun readHeader(stream: InputStream): Long {
    val header = ByteArray(28, { 0.toByte() })
    if (stream.read(header) < 28) {
      throw IOException("Can't read header of MO file, input stream ends before header is complete!")
    }
    val magic = header.slice(0 until MAGIC.size)
    bigEndian = if (magic == MAGIC) false
      else if (magic == MAGIC.reversed()) true
      else throw IOException("Not a MO file, magic bytes are incorrect!")

    val headerInts = header.slice(MAGIC.size until header.size).toLongList(bigEndian)

    formatRev = headerInts[0]
    numStrings = if (headerInts[1] > Int.MAX_VALUE)
      throw NotImplementedError("Reading MO files containing more than ${Int.MAX_VALUE} strings is not implemented (this file claims to contain $headerInts[1] strings)!")
      else headerInts[1].toInt()
    offsetOrigStrings = headerInts[2]
    offsetTranslatedStrings = headerInts[3]
    sizeHashingTable = headerInts[4]
    offsetHashingTable = headerInts[5]

    return header.size.toLong()
  }
}

private fun safeRead(stream: InputStream, b: ByteArray): Int {
  val readBytes = stream.read(b)
  if (readBytes < b.size) {
    throw IOException("File ended unexpectedly")
  }
  return readBytes
}

private fun safeSkip(stream: InputStream, n: Long): Long {
  val skippedBytes = stream.skip(n)
  if (skippedBytes < n) {
    throw IOException("Could not skip over $n bytes (maybe the file ended unexpectedly)")
  }
  return skippedBytes
}

/**
 * Returns a MsgId for a string as it is saved in a *.mo file (context and EOT byte, then the string)
 */
private fun ByteArray.toMsgId(): MsgId {
  val string = this.toString(StandardCharsets.UTF_8)
  val eotIndex = string.indexOf('\u0004')
  if (eotIndex >= 0) {
    return MsgId(MsgStr(string.substring(eotIndex + 1).split('\u0000')), string.substring(0, eotIndex))
  }
  return MsgId(MsgStr(string.split('\u0000')), null)
}

private fun List<Byte>.toLongList(bigEndian: Boolean): List<Long> {
  val result = mutableListOf<Long>()
  for (i in 0 until size step 4) {
    result.add(FourBytes(get(i), get(i + 1), get(i + 2), get(i + 3)).getLongValue(bigEndian))
  }
  return result.toList()
}

private class FourBytes(val a: Byte, val b: Byte, val c: Byte, val d: Byte) {
  fun getLongValue(bigEndian: Boolean): Long {
    if (bigEndian) {
      return (((d.toUnsigned().shl(8) + c.toUnsigned()).shl(8) + b.toUnsigned()).shl(8) + a.toUnsigned())
    } else {
      return FourBytes(d, c, b, a).getLongValue(!bigEndian)
    }
  }
}

private fun Byte.toUnsigned(): Long {
  return toLong().and(0xFF)
}
