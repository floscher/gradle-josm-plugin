package org.openstreetmap.josm.gradle.plugin.common

/**
 * An iterator that splits the given string into chunks.
 *
 * The first chunk is allowed to be at most [maxNumBytes] bytes long (when represented as UTF-8),
 * the rest of the chunks can be up to [successiveMaxNumBytes] bytes long (when represented as UTF-8).
 *
 * This is mainly used to split overlong lines in `MANIFEST.MF` files, because these files have a requirement
 * that each line must be at most 72 UTF-8 bytes (!) long.
 *
 * @param original the original string that should be split
 * @param maxNumBytes the maximum number of bytes that the first chunk is allowed to have
 * @param successiveMaxNumBytes the number of bytes that all chunks except the first one
 *  are allowed to have (defaults to [maxNumBytes])
 */
public class SplitStringToMaxByteLengthIterator(
  original: String,
  private val maxNumBytes: Int,
  private val successiveMaxNumBytes: Int = maxNumBytes
): Iterator<String> {
  private companion object {
    private const val MAX_BYTES_PER_CODEPOINT = 4
  }

  private val bytes = original.encodeToByteArray()
  private var offset: Int = 0

  init {
    require(maxNumBytes >= MAX_BYTES_PER_CODEPOINT && successiveMaxNumBytes >= MAX_BYTES_PER_CODEPOINT) {
      "Splitting to lengths below $MAX_BYTES_PER_CODEPOINT bytes is not supported, " +
        "because you can't split all UTF-8 strings to such short chunks!"
    }
  }

  /**
   * @return `true` iff there are more chunks available through the [next] method
   */
  override fun hasNext(): Boolean = offset < bytes.size

  /**
   * @return the next chunk
   * @throws IllegalArgumentException
   */
  override fun next(): String {
    require(hasNext()) { "Can't iterate past the end of the string!" }
    val offset: Int = offset
    val currentMaxNumBytes = if (offset <= 0) maxNumBytes else successiveMaxNumBytes
    val nextOffset: Int = (
      ((offset + currentMaxNumBytes).coerceAtMost(bytes.size) downTo (offset + 1))
        .take(MAX_BYTES_PER_CODEPOINT)
        .firstOrNull { it == bytes.size || bytes[it].isFirstByteOfCharacter() }
      // If there are four or more continuation bytes in a row, then the processed string is not UTF-8!
      // This should never happen, because Kotlin should normally replace such illegal byte sequences
      // in strings with the https://en.wikipedia.org/wiki/Replacement_character
        ?: throw IllegalArgumentException(
          "Can't split the given string to max. $currentMaxNumBytes bytes. It is not a valid UTF-8 string! ${
            bytes.sliceArray(offset until (offset + currentMaxNumBytes).coerceAtMost(bytes.size))
              .joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') }
          }" +
            "\n Offset is $offset, ${bytes.joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') }}"
        )
      )
    this.offset = nextOffset
    return bytes.sliceArray(offset until nextOffset).decodeToString()
  }

  /**
   * @return `true` iff this byte can occur in the UTF-8 byte representation of a character as the first byte
   */
  private fun Byte.isFirstByteOfCharacter() = toUByte().let {
    it < 0x80u || it > 0xBFu
  }
}
