package org.openstreetmap.josm.gradle.plugin.i18n.io

/**
 * Represents a file format for i18n that allows for encoding strings and their translations into a [ByteArray]
 * and decoding them again from such a [ByteArray].
 */
public interface I18nFileDecoder {
  public fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr>
}
