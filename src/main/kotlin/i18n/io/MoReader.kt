package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Reads the strings contained inside a *.mo file.
 * @param moFileURL the URL of the *.mo file that you want to read
 */
@ExperimentalUnsignedTypes
class MoReader(private val moFileURL: URL) {
  companion object {
    /**
     * The big-endian magic bytes of *.mo files (little-endian would be reversed)
     */
    @JvmStatic
    val BE_MAGIC: ByteArray = listOf(0x95, 0x04, 0x12, 0xde).map { it.toUByte().toByte() }.toByteArray()
  }

  private var bigEndian: Boolean = true
  private var formatRev: UInt = 0u
  private var numStrings: Int = 0
  private var offsetOrigStrings: UInt = 0u
  private var offsetTranslatedStrings: UInt = 0u
  private var sizeHashingTable: UInt = 0u
  private var offsetHashingTable: UInt = 0u

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
      var stream1Pos: UInt = readHeader(s1)
      stream2.use { s2 ->
        var stream2Pos: UInt = 0u

        stream1Pos += s1.skipAllOrException(offsetOrigStrings - stream1Pos)

        // Read msgid strings
        val stringLengthOffset = ByteArray(8) { 0 }
        val msgIds: MutableList<MsgId> = mutableListOf()
        for (i in 0 until numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset).toUInt()
          val stringDescriptor = stringLengthOffset.toUIntList(bigEndian)
          val stringLength = stringDescriptor[0].toInt()
          if (stringLength < 0) {
            throw NotImplementedError("Reading strings longer than ${Int.MAX_VALUE} is not implemented! You are trying to read one of length ${stringLength.toUInt()}!")
          }

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          val stringBytes = ByteArray(stringLength) {0}
          stream2Pos += s2.readAllOrException(stringBytes).toUInt()

          msgIds.add(stringBytes.toMsgId())
        }

        stream1Pos += s1.skipAllOrException(offsetTranslatedStrings - stream1Pos)

        // Read msgstr strings
        for (i in 0 until numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset).toUInt()
          val stringDescriptor = stringLengthOffset.toUIntList(bigEndian)

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          val stringBytes = ByteArray(stringDescriptor[0].toInt()) {0}
          stream2Pos += s2.readAllOrException(stringBytes).toUInt()

          stringMap[msgIds[i]] = MsgStr(String(stringBytes, StandardCharsets.UTF_8).split('\u0000'))
        }
      }
    }
    return stringMap.toMap()
  }

  /**
   * Read the file header from the given input stream
   */
  private fun readHeader(stream: InputStream): UInt {
    val header = ByteArray(28) { 0 }
    val actualHeaderSize = stream.read(header)
    if (actualHeaderSize != header.size) {
      throw IOException(
        "Can't read header of MO file! When trying to read the header, $actualHeaderSize bytes were read instead of ${header.size}." +
        if (actualHeaderSize < header.size) "\nThe input stream ended before header was complete!" else ""
      )
    }
    val magic = header.sliceArray(0 until BE_MAGIC.size)
    bigEndian = when {
      BE_MAGIC.contentEquals(magic) -> true
      BE_MAGIC.reversedArray().contentEquals(magic) -> false
      else -> throw IOException("Not a MO file, magic bytes are incorrect!")
    }
    val headerInts = header.sliceArray(magic.size until header.size).toUIntList(bigEndian)
    formatRev = headerInts[0]
    numStrings = if (headerInts[1] > Int.MAX_VALUE.toUInt())
      throw NotImplementedError("Reading MO files containing more than ${Int.MAX_VALUE} strings is not implemented (this file claims to contain $headerInts[1] strings)!")
      else headerInts[1].toInt()
    offsetOrigStrings = headerInts[2]
    offsetTranslatedStrings = headerInts[3]
    sizeHashingTable = headerInts[4]
    offsetHashingTable = headerInts[5]

    return header.size.toUInt()
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
 * Four [Byte] values are combined to form one [UInt] value (either as little endian or as big endian).
 * If the size of the [ByteArray] is not a multiple of 4, the last bytes are ignored.
 * See [FourBytes] for details on how the byte values are combined.
 */
@ExperimentalUnsignedTypes
internal fun ByteArray.toUIntList(bigEndian: Boolean): List<UInt> = (0 until size - 3 step 4)
  .map{ FourBytes(get(it), get(it + 1), get(it + 2), get(it + 3)).getUIntValue(bigEndian) }
