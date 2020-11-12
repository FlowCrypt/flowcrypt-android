/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.text.InputFilter
import android.widget.TextView

/**
 * @author Denis Bondarenko
 *         Date: 11/11/20
 *         Time: 4:35 PM
 *         E-mail: DenBond7@gmail.com
 */
fun TextView.addInputFilter(inputFilter: InputFilter) {
  filters = (filters ?: emptyArray<InputFilter>()) + arrayOf(inputFilter)
}