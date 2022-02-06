package org.openstreetmap.josm.gradle.plugin.common

import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SplitStringToMaxByteLengthIteratorTest {

  companion object {
    private const val ONE_BYTE_CODEPOINT = "A"
    private const val TWO_BYTE_CODEPOINT = "Ա"
    private const val THREE_BYTE_CODEPOINT = "অ"
    private const val FOUR_BYTE_CODEPOINT = "\uD83E\uDDD1"

    private val ALL_POSSIBLE_CODEPOINT_SIZES = arrayOf(
      ONE_BYTE_CODEPOINT, TWO_BYTE_CODEPOINT, THREE_BYTE_CODEPOINT, FOUR_BYTE_CODEPOINT
    )
  }

  @Test
  fun testSplittingStringWithCodepointsOfDifferentLengths() {
    val stringToTest = "$FOUR_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT" +
      "$FOUR_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT"

    testSplit(
      listOf(
        FOUR_BYTE_CODEPOINT, THREE_BYTE_CODEPOINT, THREE_BYTE_CODEPOINT, TWO_BYTE_CODEPOINT,
        FOUR_BYTE_CODEPOINT, "$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT",
      ),
      stringToTest,
      4
    )

    testSplit(
      listOf(
        FOUR_BYTE_CODEPOINT, THREE_BYTE_CODEPOINT, "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT",
        "$FOUR_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT", "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT", ONE_BYTE_CODEPOINT,
      ),
      stringToTest,
      5
    )

    testSplit(
      listOf(
        FOUR_BYTE_CODEPOINT, "$THREE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$TWO_BYTE_CODEPOINT$FOUR_BYTE_CODEPOINT",
        "$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT", ONE_BYTE_CODEPOINT,
      ),
      stringToTest,
      6
    )

    testSplit(
      listOf(
        "$FOUR_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT",
        "$FOUR_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT", "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT",
      ),
      stringToTest,
      7
    )

    testSplit(
      listOf(
        "$FOUR_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT",
        "$FOUR_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT",
      ),
      stringToTest,
      8
    )

    testSplit(
      listOf(
        "$FOUR_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT", "$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT$FOUR_BYTE_CODEPOINT",
        "$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT",
      ),
      stringToTest,
      9
    )

    testSplit(
      listOf(
        "$FOUR_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT",
        "$TWO_BYTE_CODEPOINT$FOUR_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT$THREE_BYTE_CODEPOINT",
        "$TWO_BYTE_CODEPOINT$ONE_BYTE_CODEPOINT",
      ),
      stringToTest,
      10
    )
  }

  @Test
  fun testSplittingWithAllCodepointsHavingSameByteLength() {
    ALL_POSSIBLE_CODEPOINT_SIZES.forEach { exampleCodepoint ->
      (4..200).forEach { numFirstChunkBytes ->
        listOf(numFirstChunkBytes, 4, 17, 25, 28, 32, 41, 200).distinct().forEach { numOtherChunkBytes ->
          testSplittingSameRepeatingCharacter(exampleCodepoint, 150, numFirstChunkBytes, numOtherChunkBytes)
        }
      }
    }
  }

  private fun Byte.isFirstByteOfCharacter() = toUByte().let {
    it < 0x80u || it > 0xBFu
  }

  private fun testSplittingSameRepeatingCharacter(char: String, numCodepoints: Int, numFirstChunkBytes: Int, numOtherChunkBytes: Int = numFirstChunkBytes) {
    require(char.encodeToByteArray().mapIndexed { index, value -> index < 4 && if (index == 0) value.isFirstByteOfCharacter() else !value.isFirstByteOfCharacter() }.all { it }) { "Only one character must be passed!" }
    /** number of bytes in [char] */
    val numBytesPerCodepoint = char.encodeToByteArray().size
    /** total number of bytes in the string that is split into chunks */
    val totalNumBytes = numCodepoints * numBytesPerCodepoint

    /** Actual number of bytes in the first chunk (might be less than the requested [numFirstChunkBytes]) */
    val numActualFirstChunkBytes = numFirstChunkBytes.coerceAtMost(totalNumBytes)
    /** Number of codepoints in the first chunk */
    val numFirstChunkCodepoints = numActualFirstChunkBytes / numBytesPerCodepoint
    /** Number of codepoints in the middle chunks (except first and (optional) incomplete chunk at the end) */
    val numFullOtherChunkCodepoints = numOtherChunkBytes / numBytesPerCodepoint
    /** Number of codepoints in the middle chunks */
    val numFullOtherChunks = (numCodepoints - numFirstChunkCodepoints) / numFullOtherChunkCodepoints
    /** Number of codepoints in the final incomplete chunk (can be 0 if there is no such last chunk) */
    val numFinalChunkCodepoints = (numCodepoints - numFirstChunkCodepoints) % numFullOtherChunkCodepoints
    /** Number of bytes int the final incomplete chunk (can be 0 if there is no such last chunk) */
    val numFinalChunkBytes = numFinalChunkCodepoints * numBytesPerCodepoint
    /*
    println(
      "Splitting $numCodepoints codepoints ($totalNumBytes bytes) into one chunk of size $numFirstChunkBytes bytes and then chunks of up to $numOtherChunkBytes" +
      "  → 1×$numFirstChunkCodepoints, $numFullOtherChunks×$numFullOtherChunkCodepoints, ${numFinalChunkCodepoints.sign}×$numFinalChunkCodepoints codepoints" +
      "  → 1×$numActualFirstChunkBytes, $numFullOtherChunks×$numOtherChunkBytes, ${numFinalChunkCodepoints.sign}×$numFinalChunkBytes bytes"
    )*/
    testSplit(
      listOf(char.repeat(numFirstChunkCodepoints))
        .plus(Array(numFullOtherChunks) { char.repeat(numFullOtherChunkCodepoints) })
        .plus(listOf(char.repeat(numFinalChunkCodepoints)).filter { it.isNotEmpty() }),
      char.repeat(numCodepoints),
      numFirstChunkBytes,
      numOtherChunkBytes,
      "Tried splitting $numCodepoints codepoints ($totalNumBytes bytes) into one chunk of size $numFirstChunkBytes bytes and then chunks of up to $numOtherChunkBytes.\n" +
      "Expected 1×$numFirstChunkCodepoints, $numFullOtherChunks×$numFullOtherChunkCodepoints, ${numFinalChunkCodepoints.sign}×$numFinalChunkCodepoints codepoints" +
      "Expected 1×$numActualFirstChunkBytes, $numFullOtherChunks×$numOtherChunkBytes, ${numFinalChunkCodepoints.sign}×$numFinalChunkBytes bytes"
    )
  }

  @Test
  public fun testTooShortToSplit() = (-10 until 4).plus(Int.MIN_VALUE).forEach {
    assertFailsWith<IllegalArgumentException>("Expected IllegalArgumentException for maxByteLength=$it") {
      SplitStringToMaxByteLengthIterator("", it)
    }
  }

  private fun testSplit(
    expectedSplit: List<String>,
    originalString: String,
    maxByteLength: Int,
    successiveMaxByteLength: Int = maxByteLength,
    failMessage: String? = null
  ) {
    assertEquals(
      expectedSplit,
      SplitStringToMaxByteLengthIterator(originalString, maxByteLength, successiveMaxByteLength).asSequence().toList(),
      failMessage
    )
  }
}
