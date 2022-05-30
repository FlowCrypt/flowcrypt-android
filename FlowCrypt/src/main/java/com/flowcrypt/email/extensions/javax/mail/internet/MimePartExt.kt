/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.jakarta.mail.internet

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimePart

fun MimePart.hasFileName(): Boolean {
  return try {
    this.fileName != null
  } catch (ex: MessagingException) {
    false
  }
}
