/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *    Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.javax.mail

import javax.mail.BodyPart
import javax.mail.MessagingException

fun BodyPart.hasFileName(): Boolean {
  return try {
    this.fileName != null
  } catch (ex: MessagingException) {
    false
  }
}
