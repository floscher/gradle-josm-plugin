package org.openstreetmap.josm.gradle.plugin.common

import kotlin.math.roundToInt

/**
 * Takes two numbers and returns a Unicode progress bar of exactly length 36:
 *   * 1 character: if [numCompleted] == [numTotal] then ▓ else ░
 *   * 25 characters: the progress bar itself (see [toRawProgressBar])
 *   * 1 character: if [numCompleted] == [numTotal] then ▓ else ░
 *   * 9 characters: percentage with 2 decimal places and percent sign
 *
 * @throws IllegalArgumentException if [numCompleted] &gt; [numTotal]
 */
@ExperimentalUnsignedTypes
public fun formatAsProgressBar(numCompleted: UInt, numTotal: UInt): String {
  require(numCompleted <= numTotal) {
    "Can't format progress bar for more than 100%!: $numCompleted / $numTotal"
  }
  val proportion = if (numTotal == 0u) 1.0 else numCompleted.toDouble() / numTotal.toDouble()
  return proportion.toRawProgressBar(25u)
    .run { if (numCompleted == numTotal) "▓$this▓" else "░$this░" } // highlight when fully completed
    .run {
      "$this ${
        (proportion * 10000).roundToInt().toString() // format to number with always 2 decimal places
          .padStart(3, '0')
          .padStart(5, ' ')
          .run { substring(0..2) + '.' + substring(3) }
      } %"
    }
}

/**
 * Converts a number in the range from `0.0` to `1.0` into a unicode progress bar of length between
 * 0 and [length] characters.
 * The progressbar uses characters `U+2588` to `U+258F` from the
 * [block elements Unicode block](https://en.wikipedia.org/wiki/Block_Elements), so 8 * [length] different
 * progress bar states are possible.
 * Numbers below `0.0` will produce an empty bar, numbers above `1.0` a full bar.
 *
 * @return A simple unicode progress bar, padded with spaces at the end, so always exactly [length] characters long
 */
@ExperimentalUnsignedTypes
private fun Double.toRawProgressBar(length: UShort = 25u): String = this
  .coerceIn(0.0, 1.0)
  .run {
    (
      "█".repeat((this * length.toInt()).toInt()) +
      when ((this * length.toInt() * 8 % 8).roundToInt()) {
        0 -> ""
        1 -> '▏'
        2 -> '▎'
        3 -> '▍'
        4 -> '▌'
        5 -> '▋'
        6 -> '▊'
        7 -> '▉'
        else -> '█'
      }
    ).padEnd(length.toInt(), ' ')
  }
