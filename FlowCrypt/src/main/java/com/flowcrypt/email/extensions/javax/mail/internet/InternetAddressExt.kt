/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.jakarta.mail.internet

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import jakarta.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 *         Date: 3/30/21
 *         Time: 10:41 AM
 *         E-mail: DenBond7@gmail.com
 */
val InternetAddress.domain: String
  get() = address.substring(address.indexOf('@') + 1)

val InternetAddress.personalOrEmail: CharSequence
  get() = if (personal.isNullOrEmpty()) address else personal

fun InternetAddress.getFormattedString(): CharSequence {
  return if (personal.isNullOrEmpty()) {
    SpannableString(address)
  } else {
    SpannableString("$personal <$address>").apply {
      setSpan(StyleSpan(Typeface.BOLD), 0, personal.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
  }
}
