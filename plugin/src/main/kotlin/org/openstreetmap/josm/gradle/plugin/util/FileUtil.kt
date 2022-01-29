package org.openstreetmap.josm.gradle.plugin.util

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import kotlin.jvm.Throws

@Throws(IOException::class)
public fun File.toBase64DataUrl(): String =
  "data:${
    Files.probeContentType(Paths.get(toURI())) ?: FileInputStream(this).use {
      URLConnection.guessContentTypeFromStream(it)
    }
  };base64,${
    Base64.getEncoder().encodeToString(readBytes())
  }"
