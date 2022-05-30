/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.extensions.jakarta.mail.internet.domain
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.InputStream

/**
 * A custom realization of [MimeMessage] that overrides <code>MessageID</code> header
 * to fit the following schema: "<unique_id@from.host.domain>" or "<unique_id@host>"
 * if the from address is not specified. Where "host" is extracted
 * from [InternetAddress.getLocalAddress]
 *
 * @author Denis Bondarenko
 *         Date: 3/30/21
 *         Time: 9:30 AM
 *         E-mail: DenBond7@gmail.com
 */
open class FlowCryptMimeMessage : MimeMessage {
  constructor(session: Session) : super(session)
  constructor(session: Session, inputStream: InputStream) : super(session, inputStream)
  constructor(mimeMessage: MimeMessage) : super(mimeMessage)

  override fun updateMessageID() {
    super.updateMessageID()
    val from = from?.firstOrNull() ?: return
    val domain = (from as? InternetAddress)?.domain ?: return
    val originalMessageId = getHeader("Message-ID").firstOrNull() ?: return
    val modifiedMessageId = originalMessageId.replace("@.*>\$".toRegex(), "@$domain>")
    setHeader("Message-ID", modifiedMessageId)
  }
}
