/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import jakarta.mail.internet.MimeMultipart

/**
 * @author Denys Bondarenko
 */
class CustomMimeMultipart constructor(contentType: String?) : MimeMultipart() {
  init {
    contentType?.let { this.contentType = it }
  }
}
