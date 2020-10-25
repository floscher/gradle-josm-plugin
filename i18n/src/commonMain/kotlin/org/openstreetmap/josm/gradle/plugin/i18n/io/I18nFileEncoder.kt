package org.openstreetmap.josm.gradle.plugin.i18n.io

public interface I18nFileEncoder {
  /**
   * Encode the [translations] given as parameter to the [I18nFileDecoder]
   * @param translations the translations that should be encoded
   * @return the encoded bytes
   */
  public fun encodeToByteArray(translations: Map<MsgId, MsgStr>): ByteArray
}
