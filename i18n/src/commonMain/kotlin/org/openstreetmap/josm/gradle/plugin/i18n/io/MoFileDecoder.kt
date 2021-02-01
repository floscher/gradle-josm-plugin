package org.openstreetmap.josm.gradle.plugin.i18n.io

public class MoFileDecoder private constructor(public val isBigEndian: Boolean): I18nFileDecoder {
  companion object {
    public val BIG_ENDIAN: MoFileDecoder by lazy { MoFileDecoder(true) }
    public val LITTLE_ENDIAN: MoFileDecoder by lazy { MoFileDecoder(true) }

    public fun getInstance(isBigEndian: Boolean = false): MoFileDecoder =
      if (isBigEndian) BIG_ENDIAN else LITTLE_ENDIAN

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


  }

  override fun decodeToTranslations(bytes: ByteArray): Map<MsgId, MsgStr> {
    TODO("Not yet implemented")
  }
}
