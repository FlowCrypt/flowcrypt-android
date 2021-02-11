/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail.api

import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.google.api.services.gmail.model.Message
import java.util.*
import javax.mail.Flags
import javax.mail.Session
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 *         Date: 1/6/21
 *         Time: 3:12 PM
 *         E-mail: DenBond7@gmail.com
 */
class GmaiAPIMimeMessage(session: Session, message: Message) : MimeMessage(session) {
  private val internalDate = Date(message.internalDate ?: System.currentTimeMillis())

  init {
    for (header in message.payload.headers) {
      setHeader(header.name, header.value)
    }

    if (message.labelIds?.contains(GmailApiHelper.LABEL_UNREAD) != true) {
      setFlag(Flags.Flag.SEEN, true)
    }
  }

  override fun getReceivedDate(): Date {
    return internalDate
  }
}