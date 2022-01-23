package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

@ExperimentalUnsignedTypes
class InputStreamTest {

  @Test
  fun testReadAllOrExceptionFail1() {
    // too short input stream
    assertThrows<IOException> {
      ByteArray(0).inputStream().readAllOrException(ByteArray(2))
    }
  }

  @Test
  fun testReadAllOrExceptionFail2() {
    // too short input stream
    assertThrows<IOException> {
      ByteArray(1).inputStream().readAllOrException(ByteArray(2))
    }
  }

  @Test
  fun testReadAllOrException() {
    // check values are read correctly
    val resultArray = ByteArray(2)
    assertEquals(2, ByteArray(2) { 42 }.inputStream().readAllOrException(resultArray))
    assertEquals(42, resultArray[0])
    assertEquals(42, resultArray[1])

    // too long stream does not hurt
    assertEquals(2, ByteArray(42).inputStream().readAllOrException(ByteArray(2)))
  }

  @Test
  fun testSkipAllOrExceptionFail1() {
    assertThrows<IOException> {
      ByteArray(0).inputStream().skipAllOrException(2u)
    }
  }

  @Test
  fun testSkipAllOrExceptionFail2() {
    assertThrows<IOException> {
      ByteArray(1).inputStream().skipAllOrException(2u)
    }
  }

  @Test
  fun testSkipAllOrException() {
    assertEquals(2u, ByteArray(2).inputStream().skipAllOrException(2u))
    assertEquals(2u, ByteArray(3).inputStream().skipAllOrException(2u))
    assertEquals(2u, ByteArray(72).inputStream().skipAllOrException(2u))
  }

  @Test
  fun testReadTwoBytesFail() {
    assertThrows<IOException> {
      ByteArray(1).inputStream().readTwoBytesAsInt()
    }
  }

  @Test
  fun testReadTwoBytes() {
    assertEquals(-1, ByteArray(0).inputStream().readTwoBytesAsInt())
    assertEquals(
      3370 /* = 13 * 256 + 42 */,
      arrayOf(13.toByte(), 42.toByte(), 72.toByte()).toByteArray().inputStream().readTwoBytesAsInt()
    )
  }
}
