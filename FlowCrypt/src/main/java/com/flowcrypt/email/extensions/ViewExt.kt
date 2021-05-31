/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * @author Denis Bondarenko
 *         Date: 5/27/20
 *         Time: 3:51 PM
 *         E-mail: DenBond7@gmail.com
 */
fun View.showKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.gone() {
  if (visibility != View.GONE) {
    visibility = View.GONE
  }
}


fun View.visible() {
  if (visibility != View.VISIBLE) {
    visibility = View.VISIBLE
  }
}

fun View.visibleOrGone(isVisible: Boolean) {
  if (isVisible) {
    visible()
  } else {
    gone()
  }
}
