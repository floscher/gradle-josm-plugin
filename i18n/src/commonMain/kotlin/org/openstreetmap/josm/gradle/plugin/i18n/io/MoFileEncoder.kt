package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Can encode a MO file for given base strings and associated translated strings.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public class MoFileEncoder private constructor(public val isBigEndian: Boolean): I18nFileEncoder {
  public companion object {
    public val BIG_ENDIAN: MoFileEncoder by lazy { MoFileEncoder(true) }
    public val LITTLE_ENDIAN: MoFileEncoder by lazy { MoFileEncoder(false) }

    public fun getInstance(isBigEndian: Boolean = false): MoFileEncoder =
      if (isBigEndian) BIG_ENDIAN else LITTLE_ENDIAN

    /**
     * Converts this object to the default byte level representation as used in *.mo files.
     * The [id] is converted according to [MsgStr.toBytes], it is separated from the context by [CONTEXT_SEPARATOR].
     */
    public fun MsgId.toBytes(): List<Byte> = if (context == null) {
      id.toBytes()
    } else {
      context.encodeToByteArray().toList() + MoFileFormat.CONTEXT_SEPARATOR.toByte() + id.toBytes()
    }

    /**
     * Converts this [MsgStr] to the byte level representation as used in *.mo files.
     * The strings for the different grammatical numbers (singular, plural(s)) are separated by [MoFileFormat.NULL_BYTE].
     * @returns this [MsgStr] represented as a [List] of [Byte]s (strings separated by [MoFileFormat.NULL_BYTE])
     */
    public fun MsgStr.toBytes(): List<Byte> = strings.joinToString(MoFileFormat.NULL_CHAR.toString()).encodeToByteArray().toList()
  }

  override fun encodeToByteArray(translations: Map<MsgId, MsgStr>): ByteArray {
    // offset 0: magic number
    val magicBytes = if (isBigEndian) MoFileFormat.BE_MAGIC else MoFileFormat.BE_MAGIC.reversed()

    val sortedOriginalMsgIds = translations.ensureUtf8EncodingInHeaderEntry().sortedBy { it.first }

    val numStrings = sortedOriginalMsgIds.size.toUInt()
    val stringsOffset = MoFileFormat.HEADER_SIZE_IN_BYTES.toUInt()
    val translationStringsOffset = stringsOffset + numStrings * 8u
    val hashTableOffset = translationStringsOffset + numStrings * 8u

    val header = listOf(
      // offset 4: file format revision
      FourBytes(0u, isBigEndian),
      // offset 8: number of strings
      FourBytes(numStrings, isBigEndian),
      // offset 12: offset of table with original strings
      FourBytes(stringsOffset, isBigEndian),
      // offset 16: offset of table with translation strings
      FourBytes(translationStringsOffset, isBigEndian),
      // offset 20: size of hashing table
      FourBytes(0u, isBigEndian),
      // offset 24: offset of hashing table
      FourBytes(hashTableOffset, isBigEndian)
    ).toBytes()
    require((header.size + magicBytes.size).toUInt() == stringsOffset) // make sure the string offset is set correctly

    /**
     * The hash table entries of the original strings in the first half,
     * then also the translated strings in the second half.
     * In the header [stringsOffset] and [translationStringsOffset] determine where the halves start and end,
     * so the entries can be put together in one list.
     */
    val stringListEntries = sortedOriginalMsgIds.map { it.first.toBytes() }
      .plus(sortedOriginalMsgIds.map { it.second.toBytes() })
      .fold(listOf<StringListEntry>()) { list, bytes ->
        list + StringListEntry(
          list.lastOrNull()?.nextOffset ?: hashTableOffset,
          bytes + MoFileFormat.NULL_CHAR.toByte()
        )
      }

    return listOf(
      magicBytes,
      header,

      stringListEntries.flatMap { listOf(
        FourBytes(it.bytes.size.toUInt() - 1u, isBigEndian), // string length
        FourBytes(it.offset, isBigEndian)) // string offset
      }.toBytes(),

      stringListEntries.flatMap { it.bytes } // the strings themselves

    ).flatten().toByteArray()
  }

  private data class StringListEntry(val offset: UInt, val bytes: List<Byte>) {
    val nextOffset: UInt = offset + bytes.size.toUInt()
  }
}
