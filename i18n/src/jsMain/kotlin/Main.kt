package org.openstreetmap.josm.gradle.i18n

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.TagConsumer
import kotlinx.html.dom.append
import kotlinx.html.js.a
import kotlinx.html.js.td
import kotlinx.html.js.th
import kotlinx.html.js.tr
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.openstreetmap.josm.gradle.i18n.MimeType.LANG
import org.openstreetmap.josm.gradle.i18n.MimeType.PO
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgId
import org.openstreetmap.josm.gradle.plugin.i18n.io.MsgStr
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFormat
import org.openstreetmap.josm.gradle.plugin.i18n.io.encodeToLangBytes
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.WindowOrWorkerGlobalScope
import org.w3c.dom.events.Event
import org.w3c.dom.get
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Promise
import kotlin.math.roundToInt

private val jsErrorMessage get() = document.getElementById("javascript-error") as HTMLDivElement
private val inputFile get() = document.getElementById("po-input-file") as HTMLInputElement
private val outputTable get() = document.getElementById("output-table") as HTMLTableElement
private val loader get() = document.getElementById("loader") as HTMLDivElement

private val REGEX_FILENAME_TO_LANGUAGE = "(.+)\\.(lang|mo|po|pot)".toRegex()

public fun main() {
  window.onload = {
    jsErrorMessage.remove()

    inputFile.onchange = {
      loader.classList.remove("display-none")

      outputTable.removeAllChildNodes()

      Promise.all(
        Array(inputFile.files?.length ?: 0) { inputFile.files?.get(it)!! }.sortedBy { it.name }.map { file ->
          Promise { resolve: (Pair<String, Map<MsgId, MsgStr>>) -> Unit, reject: (Throwable) -> Unit ->
            try {
              val reader = FileReader()
              reader.onloadend = {
                try {
                  val language = REGEX_FILENAME_TO_LANGUAGE.matchEntire(file.name)?.let { matchResult -> matchResult.groupValues[1] } ?: file.name
                  val bytes = (reader.result as ArrayBuffer).takeIf { it.byteLength <= 20 * 1024 * 1024 }?.toByteArray() ?: throw IllegalArgumentException("This file is bigger than 20 MiB!")
                  resolve(language to PoFormat().decodeToTranslations(bytes))
                } catch (t: Throwable) {
                  reject(Exception("File ${file.name}: ${t.message}", t))
                }
              }
              reader.readAsArrayBuffer(file)
            } catch (t: Throwable) {
              reject(Exception("File ${file.name}: ${t.message}", t))
            }
          }
        }.toTypedArray()
      ).then {
        outputTable.fillTableWith(it.toMap())
        loader.classList.add("display-none")
      }.catch {
        loader.classList.add("display-none")
        outputTable.append {
          tr {
            attributes["class"] = "error"
            td {
              text(it.message ?: "Unknown error!")
            }
          }
        }
      }
    }
    inputFile.onchange?.invoke(Event("change"))
  }
}

private fun HTMLTableElement.fillTableWith(translations: Map<String, Map<MsgId, MsgStr>>) {
  val poFiles: Map<String, ByteArray> = translations.entries.map { it.key to PoFormat().encodeToByteArray(it.value) }.toMap()
  val langFiles: Map<String, ByteArray> = encodeToLangBytes(translations)

  append {
    th {
      text("File name")
    }
    th {
      text("number of strings")
    }
    th {
      colSpan = "2"
      text("Download converted files")
    }
  }

  (poFiles.keys + langFiles.keys).distinct().sorted().forEach {
    append {
      tr {
        td {
          text(it)
        }
        td {
          translations[it]?.size?.let { text("$it strings") }
        }
        td {
          poFiles[it]?.let { bytes ->
            createDownloadLink(PO, it, "PO file", bytes)
          }
        }
        td {
          langFiles[it]?.let { bytes ->
            createDownloadLink(LANG, "${it}.lang", "LANG file", bytes)
          }
        }
      }
    }
  }
}

private enum class MimeType(val type: String) {
  PO("text/x-gettext-translation;charset=UTF-8"),
  MO("application/x-gettext-translation"),
  LANG("application/x-josm-lang")
}

private fun TagConsumer<HTMLElement>.createDownloadLink(
  mimeType: MimeType,
  downloadName: String,
  label: String,
  bytes: ByteArray
): HTMLAnchorElement = a ("data:${mimeType.type};base64,${bytes.encodeToBase64()}"){
  attributes["download"] = downloadName
  text(label)
}

private fun Int.formatBytes() = if (this >= 1024) {
  "${((this / 1024.0) * 100).roundToInt() / 100.0} kiB"
} else "$this Bytes"

private fun Node.removeAllChildNodes() = childNodes.toList().forEach { this.removeChild(it) }

private fun NodeList.toList(): List<Node> = (0 until length).map { get(it)!! }

private fun ArrayBuffer.toByteArray(): ByteArray = Uint8Array(this).let { arr -> ByteArray(arr.length) { arr[it] } }

public external val self: WindowOrWorkerGlobalScope

@OptIn(ExperimentalUnsignedTypes::class)
public fun ByteArray.encodeToBase64(): String = self.btoa(this.map { it.toUByte().toInt().toChar() }.joinToString(""))
