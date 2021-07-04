/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.javax.mail.internet

import javax.mail.MessagingException
import javax.mail.internet.MimePart

fun MimePart.hasFileName(): Boolean {
  return try {
    this.fileName != null
  } catch (ex: MessagingException) {
    false
  }
}
