/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.text.InputFilter
import android.widget.TextView

/**
 * @author Denys Bondarenko
 */
fun TextView.addInputFilter(inputFilter: InputFilter) {
  filters = (filters ?: emptyArray<InputFilter>()) + arrayOf(inputFilter)
}
