/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.javax.mail.internet

import javax.mail.Address
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 *         Date: 12/30/21
 *         Time: 1:42 PM
 *         E-mail: DenBond7@gmail.com
 */
fun MimeMessage.getAddresses(type: Message.RecipientType): List<String> {
  return getRecipients(type)
    ?.mapNotNull { (it as? InternetAddress)?.address?.lowercase() } ?: emptyList()
}

fun MimeMessage.getFromAddress(): String {
  return (from.first() as? InternetAddress)?.address?.lowercase()
    ?: throw IllegalStateException("'from' address is undefined")
}

fun MimeMessage.getMatchingRecipients(
  type: Message.RecipientType,
  list: Iterable<String>
): Array<Address> {
  return getRecipients(type)?.filter {
    (it as? InternetAddress)?.address?.lowercase() in list
  }?.toTypedArray() ?: emptyArray()
}
