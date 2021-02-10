package org.openstreetmap.josm.gradle.plugin.i18n.util

import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class TextUtilTest {
  @Test
  fun testFormatAsProgressBar() {
    assertEquals("░                         ░   0.00 %", formatAsProgressBar(0u, 1u))
    assertEquals("▒█████████████████████████▒ 100.00 %", formatAsProgressBar(0u, 0u))
    assertEquals("▒█████████████████████████▒ 100.00 %", formatAsProgressBar(1u, 1u))

    assertEquals("░▏                        ░   0.50 %", formatAsProgressBar(1u, 200u))
    assertEquals("░▎                        ░   1.00 %", formatAsProgressBar(2u, 200u))
    assertEquals("░▎                        ░   1.25 %", formatAsProgressBar(1249u, 100000u))
    assertEquals("░▍                        ░   1.25 %", formatAsProgressBar(125u, 10000u))
    assertEquals("░▍                        ░   1.50 %", formatAsProgressBar(3u, 200u))

    assertEquals("░██████████▌              ░  42.00 %", formatAsProgressBar(42u, 100u))
    assertEquals("░████████████▌            ░  50.00 %", formatAsProgressBar(2000u, 4000u))

    (98250u..100000u).forEach { i ->
      val formattedProgressbar = formatAsProgressBar(i, 100000u)
      val percentage = (i.toLong() / 10.0).roundToLong().toString()
        .let { "${it.substring(0, it.length - 2)}.${it.substring(it.length - 2)} %" }
        .padStart(8, ' ')
      if (i < 98750u) {
        assertEquals("░████████████████████████▋░ $percentage", formattedProgressbar)
      } else if (i < 99250u) {
        assertEquals("░████████████████████████▊░ $percentage", formattedProgressbar)
      } else if (i < 99750u) {
        assertEquals("░████████████████████████▉░ $percentage", formattedProgressbar)
      } else if (i < 99995u) {
        assertEquals("░█████████████████████████░ $percentage",  formattedProgressbar)
      } else {
        assertEquals("▒█████████████████████████▒ 100.00 %", formattedProgressbar)
      }
    }
    assertEquals("▒█████████████████████████▒ 100.00 %", formatAsProgressBar(1729u, 1729u))
  }


}
