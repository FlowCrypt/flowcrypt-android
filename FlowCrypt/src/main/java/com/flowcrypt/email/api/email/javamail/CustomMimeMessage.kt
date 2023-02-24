/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import com.flowcrypt.email.api.email.JavaEmailConstants
import jakarta.mail.Session
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.util.Properties

/**
 * It's a custom realization of [MimeMessage] which has an own realization of creation [InternetHeaders]
 *
 * @author Denys Bondarenko
 */
class CustomMimeMessage constructor(
  session: Session = Session.getInstance(Properties()),
  rawHeaders: String?
) : MimeMessage(session) {
  init {
    headers = InternetHeaders(ByteArrayInputStream(rawHeaders?.toByteArray() ?: "".toByteArray()))
  }

  fun setMessageId(msgId: String) {
    setHeader(JavaEmailConstants.HEADER_MESSAGE_ID, msgId)
  }
}
