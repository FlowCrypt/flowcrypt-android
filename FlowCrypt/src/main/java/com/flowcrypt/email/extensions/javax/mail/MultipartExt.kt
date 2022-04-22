/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.javax.mail

import javax.mail.Multipart

/**
 * @author Denis Bondarenko
 *         Date: 4/19/22
 *         Time: 7:58 PM
 *         E-mail: DenBond7@gmail.com
 */
fun Multipart.hasPartWithHtmlText(): Boolean {
  for (i in 0 until count) {
    if (getBodyPart(i).isHtmlText()) {
      return true
    }
  }
  return false
}
