package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Reads the strings contained inside a *.mo file.
 * @param moFileURL the URL of the *.mo file that you want to read
 */
class MoReader(private val moFileURL: URL) {
  companion object {
    /**
     * The big-endian magic bytes of *.mo files (little-endian would be reversed)
     */
    val BE_MAGIC: List<Byte> = listOf(/* 0x95 */ -107, /* 0x04 */ 4, /* 0x12 */ 18, /* 0xde */ -34)
  }

  private var bigEndian: Boolean = true
  private var formatRev: Long = 0
  private var numStrings: Int = 0
  private var offsetOrigStrings: Long = 0
  private var offsetTranslatedStrings: Long = 0
  private var sizeHashingTable: Long = 0
  private var offsetHashingTable: Long = 0

  /**
   * Reads the *.mo file at the given [URL] and returns the contained strings as a [Map] from [MsgId]s to [MsgStr]s.
   * @throws NotImplementedError If the file contains a string longer than [Int.MAX_VALUE]
   * @throws IOException If the
   */
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

        stream1Pos += s1.skipAllOrException(offsetOrigStrings - stream1Pos)

        // Read msgid strings
        var stringBytes: ByteArray
        val stringLengthOffset = ByteArray(8) { 0 }
        var stringDescriptor: List<Long>
        val msgIds: MutableList<MsgId> = mutableListOf()
        for (i in 0 until numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset)
          stringDescriptor = stringLengthOffset.toList().toLongList(bigEndian)
          if (stringDescriptor[0] > Int.MAX_VALUE) {
            throw NotImplementedError("Strings longer than ${Int.MAX_VALUE} can not be read! You are trying to read one of length ${stringDescriptor[0]}")
          }

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          stringBytes = ByteArray(stringDescriptor[0].toInt()) {0}
          stream2Pos += s2.readAllOrException(stringBytes)

          msgIds.add(stringBytes.toMsgId())
        }

        stream1Pos += s1.skipAllOrException(offsetTranslatedStrings - stream1Pos)

        // Read msgstr strings
        for (i in 0 until numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset)
          stringDescriptor = stringLengthOffset.toList().toLongList(bigEndian)

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          stringBytes = ByteArray(stringDescriptor[0].toInt()) {0}
          stream2Pos += s2.readAllOrException(stringBytes)

          stringMap[msgIds[i]] = MsgStr(String(stringBytes, StandardCharsets.UTF_8).split('\u0000'))
        }
      }
    }
    return stringMap.toMap()
  }

  /**
   * Read the file header from the given input stream
   */
  private fun readHeader(stream: InputStream): Long {
    val header = ByteArray(28) { 0.toByte() }
    if (stream.read(header) < 28) {
      throw IOException("Can't read header of MO file, input stream ends before header is complete!")
    }
    val magic = header.slice(0 until BE_MAGIC.size)
    bigEndian = when (magic) {
      BE_MAGIC -> true
      BE_MAGIC.reversed() -> false
      else -> throw IOException("Not a MO file, magic bytes are incorrect!")
    }
    val headerInts = header.slice(BE_MAGIC.size until header.size).toLongList(bigEndian)

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

/**
 * Returns a MsgId for a string as it is saved in a *.mo file (context and EOT byte, then the string)
 */
internal fun ByteArray.toMsgId(contextSeparator: Char = '\u0004', pluralSeparator: Char = '\u0000'): MsgId {
  val string = this.toString(StandardCharsets.UTF_8)
  val csIndex = string.indexOf(contextSeparator)
  return if (csIndex >= 0) {
    MsgId(
      MsgStr(string.substring(csIndex + 1).split(pluralSeparator)),
      string.substring(0, csIndex)
    )
  } else {
    MsgId(MsgStr(string.split(pluralSeparator)))
  }
}

/**
 * Converts a list of bytes to a list of long values.
 * Four [Byte] values are combined to form one [Long] value (either as little endian or as big endian).
 * See [FourBytes] for details on how the byte values are combined.
 */
internal fun List<Byte>.toLongList(bigEndian: Boolean): List<Long> {
  val result = mutableListOf<Long>()
  for (i in 0 until size - 3 step 4) {
    result.add(FourBytes(get(i), get(i + 1), get(i + 2), get(i + 3)).getLongValue(bigEndian))
  }
  return result.toList()
}
