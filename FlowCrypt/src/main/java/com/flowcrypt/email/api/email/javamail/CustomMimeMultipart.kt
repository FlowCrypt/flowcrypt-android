/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import jakarta.mail.internet.MimeMultipart

/**
 * @author Denis Bondarenko
 *         Date: 2/19/21
 *         Time: 4:27 PM
 *         E-mail: DenBond7@gmail.com
 */
class CustomMimeMultipart constructor(contentType: String?) : MimeMultipart() {
  init {
    contentType?.let { this.contentType = it }
  }
}
