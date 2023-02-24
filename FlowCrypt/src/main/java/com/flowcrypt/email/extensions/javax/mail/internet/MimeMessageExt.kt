/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.jakarta.mail.internet

import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

/**
 * @author Denys Bondarenko
 */
fun MimeMessage.getAddresses(type: Message.RecipientType): List<String> {
  return getRecipients(type)
    ?.mapNotNull { (it as? InternetAddress)?.address?.lowercase() } ?: emptyList()
}

fun MimeMessage.getFromAddress(): InternetAddress {
  return (from.first() as? InternetAddress)
    ?: throw IllegalStateException("'from' address is undefined")
}
