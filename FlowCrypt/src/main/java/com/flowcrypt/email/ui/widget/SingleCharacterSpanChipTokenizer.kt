/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context

import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.Chip
import com.hootsuite.nachos.chip.ChipCreator
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer

/**
 * Define a custom chip separator in [NachoTextView]
 *
 * @author DenBond7
 * Date: 19.05.2017
 * Time: 14:14
 * E-mail: DenBond7@gmail.com
 */
class SingleCharacterSpanChipTokenizer<C : Chip>
@JvmOverloads constructor(context: Context,
                          chipCreator: ChipCreator<C>,
                          chipClass: Class<C>,
                          private val symbol: Char = CHIP_SEPARATOR_WHITESPACE) : SpanChipTokenizer<C>(context,
    chipCreator, chipClass) {
  override fun findTokenStart(text: CharSequence, cursor: Int): Int {
    var i = cursor

    while (i > 0 && text[i - 1] != symbol) {
      i--
    }
    while (i < cursor && text[i] == symbol) {
      i++
    }

    return i
  }

  override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
    var i = cursor
    val len = text.length

    while (i < len) {
      if (text[i] == symbol) {
        return i
      } else {
        i++
      }
    }

    return len
  }

  companion object {
    const val CHIP_SEPARATOR_WHITESPACE = ' '
  }
}
