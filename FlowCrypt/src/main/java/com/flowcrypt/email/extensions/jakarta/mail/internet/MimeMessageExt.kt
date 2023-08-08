/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.jakarta.mail.internet

import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

/**
 * @author Denys Bondarenko
 */
fun MimeMessage.getFromAddress(): InternetAddress {
  return (from.first() as? InternetAddress)
    ?: throw IllegalStateException("'from' address is undefined")
}
