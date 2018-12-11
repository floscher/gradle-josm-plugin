package org.openstreetmap.josm.gradle.plugin.i18n.io

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * Reads the strings contained inside a *.mo file.
 *
 * @property stream1 reads the indices of the strings
 * @property stream2 reads the actual strings
 */
@ExperimentalUnsignedTypes
class MoReader private constructor(private val stream1: InputStream, private val stream2: InputStream) {

  /**
   * @param moFileURL the URL of the *.mo file that you want to read
   */
  constructor(moFileURL: URL): this(moFileURL.openStream(), moFileURL.openStream())
  internal constructor(bytes: ByteArray): this(ByteArrayInputStream(bytes), ByteArrayInputStream(bytes))

  companion object {
    /**
     * The big-endian magic bytes of *.mo files (little-endian would be reversed)
     */
    @JvmStatic
    val BE_MAGIC: ByteArray = listOf(0x95, 0x04, 0x12, 0xde).map { it.toUByte().toByte() }.toByteArray()

    /**
     * 7 times 4 bytes (≙ 7 32bit numbers)
     */
    const val HEADER_SIZE_IN_BYTES = 28u
  }

  class HeaderValues(val isBigEndian: Boolean, uints: List<UInt>) {
    init {
      require(uints.size == 6)
    }
    val formatRev: UInt = uints[0]
    val numStrings: Int = uints[1].toInt()
    val offsetOrigStrings: UInt = uints[2]
    val offsetTranslatedStrings: UInt = uints[3]
    val sizeHashingTable: UInt = uints[4]
    val offsetHashingTable: UInt = uints[5]
    init {
      if (this.numStrings < 0) {
        throw NotImplementedError("Reading MO files containing more than ${Int.MAX_VALUE} strings is not implemented (this file claims to contain $numStrings strings)!")
      }
    }
  }

  /**
   * Reads the *.mo file at the given [URL] and returns the contained strings as a [Map] from [MsgId]s to [MsgStr]s.
   * @throws NotImplementedError If the file contains a string longer than [Int.MAX_VALUE]
   * @throws IOException If the
   */
  fun readFile(): Map<MsgId, MsgStr> {
    val stringMap: MutableMap<MsgId, MsgStr> = mutableMapOf()
    stream1.use { s1 ->
      // Read the header (sets the header fields)
      val header: HeaderValues = readHeader(s1)
      var stream1Pos: UInt = HEADER_SIZE_IN_BYTES
      stream2.use { s2 ->
        var stream2Pos: UInt = 0u

        stream1Pos += s1.skipAllOrException(header.offsetOrigStrings - stream1Pos)

        // Read msgid strings
        val stringLengthOffset = ByteArray(8) { 0 }
        val msgIds: MutableList<MsgId> = mutableListOf()
        for (i in 0 until header.numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset).toUInt()
          val stringDescriptor = stringLengthOffset.toUIntList(header.isBigEndian)
          val stringLength = stringDescriptor[0].toInt()
          if (stringLength < 0) {
            throw NotImplementedError("Reading strings longer than ${Int.MAX_VALUE} is not implemented! You are trying to read one of length ${stringLength.toUInt()}!")
          }

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          val stringBytes = ByteArray(stringLength) {0}
          stream2Pos += s2.readAllOrException(stringBytes).toUInt()

          msgIds.add(stringBytes.toMsgId())
        }

        stream1Pos += s1.skipAllOrException(header.offsetTranslatedStrings - stream1Pos)

        // Read msgstr strings
        for (i in 0 until header.numStrings) {
          stream1Pos += s1.readAllOrException(stringLengthOffset).toUInt()
          val stringDescriptor = stringLengthOffset.toUIntList(header.isBigEndian)

          stream2Pos += s2.skipAllOrException(stringDescriptor[1] - stream2Pos)
          val stringBytes = ByteArray(stringDescriptor[0].toInt()) {0}
          stream2Pos += s2.readAllOrException(stringBytes).toUInt()

          stringMap[msgIds[i]] = MsgStr(String(stringBytes, Charsets.UTF_8).split(MsgStr.GRAMMATICAL_NUMBER_SEPARATOR))
        }
      }
    }
    return stringMap.toMap()
  }

  /**
   * Read the file header from the given input stream
   */
  private fun readHeader(stream: InputStream): HeaderValues {
    val header = ByteArray(HEADER_SIZE_IN_BYTES.toInt()) { 0 }
    val actualHeaderSize = stream.read(header)
    if (actualHeaderSize != header.size) {
      throw IOException(
        "Can't read header of MO file! When trying to read the header, $actualHeaderSize bytes were read instead of ${header.size}." +
        if (actualHeaderSize < header.size) "\nThe input stream ended before header was complete!" else ""
      )
    }
    val magic = header.sliceArray(0 until BE_MAGIC.size)
    val isBigEndian = when {
      BE_MAGIC.contentEquals(magic) -> true
      BE_MAGIC.reversedArray().contentEquals(magic) -> false
      else -> throw IOException("Not a MO file, magic bytes are incorrect!")
    }
    val headerInts = header.sliceArray(magic.size until header.size).toUIntList(isBigEndian)

    return HeaderValues(isBigEndian, headerInts)
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
