package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.IOException

@ExperimentalUnsignedTypes
class InputStreamTest {

  @Test(expected = IOException::class)
  fun testReadAllOrExceptionFail1() {
    // too short input stream
    ByteArray(0).inputStream().readAllOrException(ByteArray(2))
  }

  @Test(expected = IOException::class)
  fun testReadAllOrExceptionFail2() {
    // too short input stream
    ByteArray(1).inputStream().readAllOrException(ByteArray(2))
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

  @Test(expected = IOException::class)
  fun testSkipAllOrExceptionFail1() {
    ByteArray(0).inputStream().skipAllOrException(2u)
  }

  @Test(expected = IOException::class)
  fun testSkipAllOrExceptionFail2() {
    ByteArray(1).inputStream().skipAllOrException(2u)
  }

  @Test
  fun testSkipAllOrException() {
    assertEquals(2, ByteArray(2).inputStream().skipAllOrException(2u))
    assertEquals(2, ByteArray(3).inputStream().skipAllOrException(2u))
    assertEquals(2, ByteArray(72).inputStream().skipAllOrException(2u))
  }

  @Test(expected = IOException::class)
  fun testReadTwoBytesFail() {
    ByteArray(1).inputStream().readTwoBytesAsInt()
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
