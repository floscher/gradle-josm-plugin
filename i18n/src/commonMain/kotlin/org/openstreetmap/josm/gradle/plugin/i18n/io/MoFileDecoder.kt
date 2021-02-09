package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.openstreetmap.josm.gradle.plugin.i18n.io.FourBytes.Companion.getFourBytesAt
/**
 * Reads the strings contained inside a *.mo file.
 *
 * See [the documentation of the MO file format](https://www.gnu.org/software/gettext/manual/html_node/MO-Files.html).
 */
@OptIn(ExperimentalUnsignedTypes::class)
public object MoFileDecoder: I18nFileDecoder {
  /**
   * Returns a MsgId for a string as it is saved in a *.mo file (context and EOT byte, then the string)
   */
  public fun ByteArray.toMsgId(): MsgId {
    val string = this.decodeToString()
    val csIndex = string.indexOf(MoFileFormat.CONTEXT_SEPARATOR)
    return if (csIndex >= 0) {
      MsgId(
        MsgStr(string.substring(csIndex + 1).split(MoFileFormat.NULL_CHAR)),
        string.substring(0, csIndex)
      )
    } else {
      MsgId(MsgStr(string.split(MoFileFormat.NULL_CHAR)))
    }
  }

  /**
   * Decodes a *.mo file to a [Map] of [MsgId]s with their corresponding [MsgStr]s.
   * See [the documentation of the MO file format](https://www.gnu.org/software/gettext/manual/html_node/MO-Files.html).
   */
  override fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr> {
    require(bytes.size > MoFileFormat.HEADER_SIZE_IN_BYTES) {
      "This MO file is too short, must be at least ${MoFileFormat.HEADER_SIZE_IN_BYTES} Bytes long!"
    }

    val isBigEndian = bytes.slice(0 until 4).let { when (it) {
      MoFileFormat.BE_MAGIC -> true
      MoFileFormat.BE_MAGIC.reversed() -> false
      else -> throw IllegalArgumentException("Not a MO file, magic bytes are incorrect!")
    }}

    val (
      /**
       * revision of the MO file format, this is usually 0, but will be ignored by this decoder
       */
      _: Int,
      /**
       * the number of strings in the file
       */
      numStrings: Int,
      /**
       * offset of table with original strings
       */
      offsetOrigStrings: Int,
      /**
       * offset of table with translation strings
       */
      offsetTranslatedStrings: Int,
      /**
       * the size of the hashing table (currently always ignored)
       */
      _: Int,
      /**
       * the offset of the hashing table (currently always ignored)
       */
      _: Int,
    ) = bytes.slice(4 until MoFileFormat.HEADER_SIZE_IN_BYTES)
      .toFourByteList()
      .map { it.getUIntValue(isBigEndian) }
      .onEachIndexed { index, value ->
        require(value <= Int.MAX_VALUE.toUInt()) {
          "The header value at ${index * 4 + 4}..${index * 4 + 7} is bigger than ${Int.MAX_VALUE} (value is $value)"
        }
      }
      .map { it.toInt() }

    return bytes.decodeStringListAt(offsetOrigStrings, numStrings, isBigEndian) { it.toByteArray().toMsgId() }
      .zip(bytes.decodeStringListAt(offsetTranslatedStrings, numStrings, isBigEndian) { MsgStr(it.toByteArray().decodeToString().split(MoFileFormat.NULL_CHAR)) })
      .toMap()
  }

  private fun <T> ByteArray.decodeStringListAt(
    startIndex: Int,
    numStrings: Int,
    isBigEndian: Boolean,
    transform: (List<Byte>) -> T
  ): List<T> = (0 until numStrings).map {
    val curIndex = startIndex + it * 8
    val stringLength = getFourBytesAt(curIndex).getUIntValue(isBigEndian)
    val stringOffset = getFourBytesAt(curIndex + 4).getUIntValue(isBigEndian)
    require(
      stringLength <= Int.MAX_VALUE.toUInt() &&
      stringOffset <= Int.MAX_VALUE.toUInt() &&
      stringLength + stringOffset <= Int.MAX_VALUE.toUInt()
    ) {
      "At bytes $curIndex..${curIndex + 7} there is a string offset or a string length that is bigger than ${Int.MAX_VALUE} (length is $stringLength, offset is $stringOffset)!"
    }
    require(stringLength + stringOffset < size.toUInt()) {
      "The string length and offset at bytes $curIndex..${curIndex + 7} are pointing to somewhere outside of the file!"
    }
    transform(slice(stringOffset.toInt() until (stringOffset + stringLength).toInt()))
  }

  private operator fun <T> List<T>.component6(): T = get(5)
}
