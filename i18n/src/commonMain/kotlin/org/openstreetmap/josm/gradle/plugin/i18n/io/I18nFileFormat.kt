package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Represents a file format for i18n that allows for encoding strings and their translations into a [ByteArray]
 * and decoding them again from such a [ByteArray].
 */
public interface I18nFileFormat {
  /**
   * Encode the [translations] given as parameter to the [I18nFileFormat]
   * @param translations the translations that should be encoded
   * @return the encoded bytes
   */
  public fun encodeToByteArray(translations: Map<MsgId, MsgStr>): ByteArray
  public fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr>
}
