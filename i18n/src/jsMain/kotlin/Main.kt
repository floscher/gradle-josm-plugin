package org.openstreetmap.josm.gradle.i18n

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.InputType
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.a
import kotlinx.html.js.code
import kotlinx.html.js.h2
import kotlinx.html.js.pre
import kotlinx.html.js.td
import kotlinx.html.js.th
import kotlinx.html.js.tr
import kotlinx.html.js.ul
import kotlinx.html.li
import kotlinx.html.style
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.openstreetmap.josm.gradle.plugin.i18n.io.PoFormat
import org.w3c.dom.HTMLDListElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.WindowOrWorkerGlobalScope
import org.w3c.dom.events.Event
import org.w3c.dom.get
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.math.roundToInt

private val jsErrorMessage get() = document.getElementById("javascript-error") as HTMLDivElement
private val inputFile get() = document.getElementById("po-input-file") as HTMLInputElement
private val poOutput get() = document.getElementById("po-output") as HTMLTableElement
private val poErrors get() = document.getElementById("po-errors") as HTMLDivElement
private val poWarnings get() = document.getElementById("po-warnings") as HTMLDivElement

public fun main() {
  window.onload = {
    jsErrorMessage.remove()

    inputFile.onchange = {
      poOutput.removeAllChildNodes()
      poOutput.append {
        th {
          text("File name")
        }
        th {
          text("number of strings")
        }
        th {
          text("Download converted files")
        }
        th {
          text("Warning messages")
        }
      }
      poErrors.removeAllChildNodes()
      poErrors.className = ""
      poWarnings.removeAllChildNodes()
      poWarnings.className = ""

      Array(inputFile.files?.length ?: 0) { inputFile.files?.get(it)!! }.sortedBy { it.name }.forEach { file ->
        val reader = FileReader()
        reader.onload = {
          try {
            val originalBytes = (reader.result as ArrayBuffer).toByteArray()
            val translations = PoFormat().decodeToTranslations(originalBytes)

            PoFormat().checkTranslations(translations).takeIf { it.isNotEmpty() }?.let {
              poWarnings.classList.add("warning")
              poWarnings.append(document.create.ul {
                for (warning in it) {
                  li { text(warning) }
                }
              })
            }
            val poBytes = PoFormat().encodeToByteArray(translations)

            poOutput.append {
              tr {
                td {
                  code {
                    text(file.name)
                  }
                  text(" (${originalBytes.size.formatBytes()})")
                }
                td {
                  style = "text-align:right"
                  text(translations.size)
                }
                td {
                  a("data:text/x-gettext-translation;charset=UTF-8;base64,${poBytes.encodeToBase64()}") {
                    this.attributes["download"] = file.name
                    text("${file.name} (${poBytes.size.formatBytes()})")
                  }
                }
              }
            }
          } catch (e: Exception) {
            poErrors.innerHTML = e.message ?: "Unknown exception!"
            poErrors.classList.add("error")
          }
        }
        reader.readAsArrayBuffer(file)
      }
    }
    inputFile.onchange?.invoke(Event("change"))
  }
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
