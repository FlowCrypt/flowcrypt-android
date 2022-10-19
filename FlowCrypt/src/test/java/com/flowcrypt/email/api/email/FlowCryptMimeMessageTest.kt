/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.extensions.jakarta.mail.internet.domain
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Properties

/**
 * @author Denis Bondarenko
 * Date: 3/30/21
 * Time: 9:54 AM
 * E-mail: DenBond7@gmail.com
 */
class FlowCryptMimeMessageTest {
  @Test
  fun testUpdateMessageIDWithFromAddress() {
    val fromAddress = InternetAddress("from@example.com")
    val expectedDomain = "example.com"
    val addressDomain = fromAddress.domain
    assertEquals(expectedDomain, addressDomain)

    val msg = FlowCryptMimeMessage(Session.getInstance(Properties()))
    msg.setFrom(fromAddress)
    msg.setText("Some text")
    msg.saveChanges()
    val messageId = msg.getHeader(JavaEmailConstants.HEADER_MESSAGE_ID).first()
    val actualDomain = extractDomainFromMessageID(messageId)
    assertEquals(expectedDomain, actualDomain)
  }

  @Test
  fun testUpdateMessageIDWithoutFromAddress() {
    val session = Session.getInstance(Properties())
    val localAddressDomain = InternetAddress.getLocalAddress(session).domain
    val msg = FlowCryptMimeMessage(session)
    msg.setText("Some text")
    msg.saveChanges()
    val messageId = msg.getHeader(JavaEmailConstants.HEADER_MESSAGE_ID).first()
    val actualDomain = extractDomainFromMessageID(messageId)
    assertEquals(localAddressDomain, actualDomain)
  }

  private fun extractDomainFromMessageID(messageId: String): String {
    return messageId
      .substring(messageId.indexOf('@') + 1)
      .replace(">", "")
  }
}
