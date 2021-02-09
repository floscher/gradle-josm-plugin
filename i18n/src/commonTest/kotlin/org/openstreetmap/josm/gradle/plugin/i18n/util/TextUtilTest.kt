package org.openstreetmap.josm.gradle.plugin.i18n.util

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class TextUtilTest {
  @Test
  fun testFormatAsProgressBar() {
    assertEquals("░                         ░   0.00 %", formatAsProgressBar(0u, 1u))
    assertEquals("▒█████████████████████████▒ 100.00 %", formatAsProgressBar(0u, 0u))

    assertEquals("░▏                        ░   0.50 %", formatAsProgressBar(1u, 200u))
    assertEquals("░▎                        ░   1.00 %", formatAsProgressBar(2u, 200u))
    assertEquals("░▎                        ░   1.25 %", formatAsProgressBar(1249u, 100000u))
    assertEquals("░▍                        ░   1.25 %", formatAsProgressBar(125u, 10000u))
    assertEquals("░▍                        ░   1.50 %", formatAsProgressBar(3u, 200u))

    assertEquals("░██████████▌              ░  42.00 %", formatAsProgressBar(42u, 100u))
    assertEquals("░████████████▌            ░  50.00 %", formatAsProgressBar(2000u, 4000u))
    assertEquals("░█████████████████████████░ 100.00 %", formatAsProgressBar(99999u, 100000u))
    assertEquals("▒█████████████████████████▒ 100.00 %", formatAsProgressBar(1729u, 1729u))
  }


}
