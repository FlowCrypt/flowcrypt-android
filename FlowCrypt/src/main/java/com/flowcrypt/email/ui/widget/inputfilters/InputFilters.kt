/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */
package com.flowcrypt.email.ui.widget.inputfilters

import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils

/**
 * @author Denys Bondarenko
 */
interface InputFilters {
  /**
   * This filter will decapitalize all the upper case letters that are added
   * through edits.
   */
  class NoCaps : InputFilter {
    override fun filter(
      source: CharSequence, start: Int, end: Int,
      dest: Spanned?, dstart: Int, dend: Int
    ): CharSequence? {
      for (i in start until end) {
        if (Character.isUpperCase(source[i])) {
          val v = CharArray(end - start)
          TextUtils.getChars(source, start, end, v, 0)
          val s = String(v).lowercase()
          return if (source is Spanned) {
            val sp = SpannableString(s)
            TextUtils.copySpansFrom(
              source,
              start, end, null, sp, 0
            )
            sp
          } else {
            s
          }
        }
      }
      return null // keep original
    }
  }

  /**
   * This filter will constrain edits not to add non-digital characters.
   */
  class OnlyDigits : InputFilter {
    override fun filter(
      source: CharSequence, start: Int, end: Int,
      dest: Spanned?, dstart: Int, dend: Int
    ): CharSequence? {
      for (i in start until end) {
        if (!Character.isDigit(source[i])) {
          return ""
        }
      }
      return null // keep original
    }
  }
}
