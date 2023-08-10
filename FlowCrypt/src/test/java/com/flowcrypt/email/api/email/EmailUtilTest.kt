/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
class EmailUtilTest {
  @Test
  fun testGetGmailBackupSearchQuery() {
    val email = "junit@example.com"
    val expectedString = """from:${email} to:${email}""" +
        """ (subject:"Your FlowCrypt Backup" """ +
        """OR subject: "Your CryptUp Backup" """ +
        """OR subject: "All you need to know about CryptUP (contains a backup)" """ +
        """OR subject: "CryptUP Account Backup") -is:spam -is:sent -is:trash"""
    val gotString = EmailUtil.getGmailBackupSearchQuery(email)
    assertEquals(expectedString, gotString)
  }

  @Test
  fun testGenNewMessagePlain() {
    val accountEntity = AccountEntity("junit@example.com")
    val subject = "subject"
    val msg = "Some plain message"
    val fromRecipients = listOf(InternetAddress(accountEntity.email))
    val toRecipients = listOf(InternetAddress("toRecipient@example.com"))
    val ccRecipients = listOf(InternetAddress("ccRecipient@example.com"))
    val bccRecipients = listOf(InternetAddress("bccRecipient@example.com"))

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = accountEntity.email,
      subject = subject,
      msg = msg,
      toRecipients = toRecipients,
      ccRecipients = ccRecipients,
      bccRecipients = bccRecipients,
      from = InternetAddress(accountEntity.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.NEW,
      uid = 1000
    )
    val newMsg = EmailUtil.prepareNewMsg(
      session = Session.getInstance(Properties()),
      info = outgoingMessageInfo
    )
    newMsg.saveChanges()

    assertEquals(subject, newMsg.subject)
    assertEquals(msg, (newMsg.content as Multipart).getBodyPart(0).content)
    assertEquals(fromRecipients, newMsg.from.toList())
    assertEquals(toRecipients, newMsg.getRecipients(Message.RecipientType.TO).toList())
    assertEquals(ccRecipients, newMsg.getRecipients(Message.RecipientType.CC).toList())
    assertEquals(bccRecipients, newMsg.getRecipients(Message.RecipientType.BCC).toList())
    assertEquals(
      LocalDate.now(), newMsg.sentDate.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    )
    assertTrue(newMsg.getHeader("MIME-Version").first() == "1.0")
  }

  @Test
  fun testGenReplyMessagePlain() {
    val session = Session.getInstance(Properties())
    val replyToMIME = MimeMessage(
      session,
      ByteArrayInputStream(REPLY_TO_MIME_MESSAGE.toByteArray())
    )
    val replyToText = replyToMIME.content as String
    val accountEntity = AccountEntity("receiver@receiver.com")
    val receivedDate = Instant.now()
    val incomingMessageInfo = IncomingMessageInfo(
      msgEntity = MessageEntity(
        email = accountEntity.email,
        folder = "INBOX",
        uid = 123,
        fromAddress = InternetAddress.toString(replyToMIME.from),
        receivedDate = receivedDate.toEpochMilli()
      ),
      text = replyToText,
      encryptionType = MessageEncryptionType.STANDARD,
      verificationResult = VerificationResult(
        hasEncryptedParts = false,
        hasSignedParts = false,
        hasMixedSignatures = false,
        isPartialSigned = false,
        keyIdOfSigningKeys = emptyList(),
        hasBadSignatures = false
      )
    )

    val replyText = "Reply text" + EmailUtil.genReplyContent(incomingMessageInfo)

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = accountEntity.email,
      //need to clarify should we override subject or not. Currently we override
      subject = "subject that will be overridden",
      msg = replyText,
      toRecipients = replyToMIME.from.toList().map { it as InternetAddress },
      from = InternetAddress(accountEntity.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.REPLY,
      uid = 1000
    )

    val replyMIME = EmailUtil.genReplyMessage(replyToMsg = replyToMIME, info = outgoingMessageInfo)
    replyMIME.saveChanges()

    assertEquals("Re: " + replyToMIME.subject, replyMIME.subject)
    assertEquals(
      replyToMIME.getRecipients(Message.RecipientType.TO).toList(),
      replyMIME.from.toList()
    )
    assertArrayEquals(replyToMIME.from, replyMIME.getRecipients(Message.RecipientType.TO))
    assertArrayEquals(
      replyToMIME.getHeader(JavaEmailConstants.HEADER_MESSAGE_ID),
      replyMIME.getHeader("In-Reply-To")
    )
    assertArrayEquals(
      replyToMIME.getHeader(JavaEmailConstants.HEADER_MESSAGE_ID),
      replyMIME.getHeader("References")
    )
  }

  companion object {
    private val REPLY_TO_MIME_MESSAGE = """
    Content-Type: text/plain; charset="UTF-8"
    To: receiver@receiver.com
    From: sender@sender.com
    Subject: Original
    Date: Mon, 25 Mar 2019 14:59:11 +0000
    Message-Id: <msg_id@sender.com>
    MIME-Version: 1.0
    
    orig message
    """.trimIndent()
  }
}
