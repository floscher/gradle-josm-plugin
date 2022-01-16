package org.openstreetmap.josm.gradle.plugin.i18n.io

import kotlin.jvm.JvmField

public object MoFileFormat {
  /**
   * The big-endian magic bytes of *.mo files (little-endian would be reversed)
   *
   * Value: `0x950412de`
   */
  @JvmField
  @OptIn(ExperimentalUnsignedTypes::class)
  public val BE_MAGIC: List<Byte> = listOf(0x95, 0x04, 0x12, 0xde).map { it.toUByte().toByte() }

  /**
   * 28 bytes = 7 × 4 bytes (≙ 7 32bit numbers)
   */
  public const val HEADER_SIZE_IN_BYTES: Int = 28

  /**
   * The `0x00` character, that is used by default to terminate strings in *.mo files.
   */
  public const val NULL_CHAR: Char = 0x00.toChar()

  /**
   * The `0x04` character that is used by default to separate the [context] from the [id]
   * in the [ByteArray] representation of this object.
   */
  public const val CONTEXT_SEPARATOR: Char = 0x04.toChar()
}
