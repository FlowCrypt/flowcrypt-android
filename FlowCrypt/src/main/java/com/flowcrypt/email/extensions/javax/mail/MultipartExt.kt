/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.jakarta.mail

import jakarta.mail.Multipart

/**
 * @author Denys Bondarenko
 */
fun Multipart.hasPartWithHtmlText(): Boolean {
  for (i in 0 until count) {
    if (getBodyPart(i).isHtmlText()) {
      return true
    }
  }
  return false
}
