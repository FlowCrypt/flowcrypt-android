/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreConnection
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.org.pgpainless.decryption_verification.isSigned
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.jetpack.workmanager.PrepareOutgoingMessagesWorker
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
@Ignore("This one is not relevant today. But I'd like to keep it for now. " +
    "It will be used as a template when a new UI test for IMAP will be added.")
class SendMsgTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val account = AccountDaoManager.getUserWithoutLetters()
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(account = account)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = account,
    keyPath = "pgp/user_without_letters@flowcrypt.test_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL
  )
  private val recipientPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/denbond7@flowcrypt.test_pub_primary.asc")
  private val bccPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/default@flowcrypt.test_fisrtKey_pub.asc")

  private val sentFolder = LocalFolder(
    account = addAccountToDatabaseRule.account.email,
    fullName = "Sent",
    folderAlias = "Sent",
    attributes = listOf("\\HasNoChildren", "\\Sent")
  )

  private val recipientWithPubKeys = listOf(
    RecipientWithPubKeys(
      RecipientEntity(
        email = addAccountToDatabaseRule.account.email,
        name = "Default"
      ),
      listOf(
        addPrivateKeyToDatabaseRule.pgpKeyRingDetails
          .toPublicKeyEntity(addAccountToDatabaseRule.account.email)
          .copy(id = 1)
      )
    ),
    RecipientWithPubKeys(
      RecipientEntity(
        email = recipientPgpKeyDetails.getUserIdsAsSingleString(),
        name = "DenBond7"
      ),
      listOf(
        recipientPgpKeyDetails
          .toPublicKeyEntity(recipientPgpKeyDetails.getUserIdsAsSingleString())
          .copy(id = 2)
      )
    ),
    RecipientWithPubKeys(
      RecipientEntity(
        email = bccPgpKeyDetails.getUserIdsAsSingleString(),
        name = "bcc"
      ),
      listOf(
        bccPgpKeyDetails
          .toPublicKeyEntity(bccPgpKeyDetails.getUserIdsAsSingleString())
          .copy(id = 3)
      )
    )
  )

  private val attachmentContent = "Some text"
  private val attachmentInfo = AttachmentInfo(
    name = ATTACHMENT_NAME,
    encodedSize = attachmentContent.length.toLong(),
    rawData = attachmentContent.toByteArray(),
    type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN,
    email = addAccountToDatabaseRule.account.email,
    id = EmailUtil.generateContentId()
  )

  @Before
  fun cleanFolderBeforeStart() {
    runBlocking { deleteAllMessagesInFolder(sentFolder.fullName) }
  }

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
    .around(AddLabelsToDatabaseRule(addAccountToDatabaseRule.account, listOf(sentFolder)))

  @Test
  fun testSendStandardMsg() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Standard Message",
      msg = "Standard Message",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.NEW,
      uid = Random.nextLong()
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(3000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val mimeMultipart = mimeMessage.content as MimeMultipart
        assertEquals(1, mimeMultipart.count)
        assertEquals(
          addAccountToDatabaseRule.account.email,
          (mimeMessage.from.firstOrNull() as? InternetAddress)?.address
        )
        assertEquals(outgoingMessageInfo.msg, mimeMultipart.getBodyPart(0).content)
      }
    }
  }

  @Test
  fun testSendStandardMsgWithAtt() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Standard Message + Att",
      msg = "Standard Message + Att",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.NEW,
      uid = Random.nextLong(),
      atts = listOf(attachmentInfo)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(3000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val multipart = mimeMessage.content as MimeMultipart
        assertEquals(outgoingMessageInfo.msg, multipart.getBodyPart(0).content)

        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(ATTACHMENT_NAME, attachmentPart.fileName)
        assertEquals(attachmentInfo.encodedSize, attachmentPart.size.toLong())
        assertEquals(attachmentInfo.type, ContentType(attachmentPart.contentType).baseType)
      }
    }
  }

  @Test
  fun testSendStandardMsgWithForwardedAtt() {
    val forwardedAttachmentInfo = prepareForwardedAttachment(
      OutgoingMessageInfo(
        account = addAccountToDatabaseRule.account.email,
        subject = "Standard Message + Att",
        msg = "Standard Message + Att",
        toRecipients = listOf(InternetAddress(addAccountToDatabaseRule.account.email)),
        from = InternetAddress(addAccountToDatabaseRule.account.email),
        encryptionType = MessageEncryptionType.STANDARD,
        messageType = MessageType.NEW,
        uid = Random.nextLong(),
        atts = listOf(attachmentInfo)
      )
    )?.copy(isForwarded = true)
    assertNotNull(forwardedAttachmentInfo)

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Fwd: Forward",
      msg = "Some forwarded text",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.FORWARD,
      uid = Random.nextLong(),
      forwardedAtts = listOf(requireNotNull(forwardedAttachmentInfo))
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(5000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val multipart = mimeMessage.content as MimeMultipart
        assertEquals(outgoingMessageInfo.msg, multipart.getBodyPart(0).content)

        val forwardedAttachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, forwardedAttachmentPart.disposition)
        assertEquals(forwardedAttachmentInfo.name, forwardedAttachmentPart.fileName)
        assertEquals(forwardedAttachmentInfo.encodedSize, forwardedAttachmentPart.size.toLong())
        assertEquals(forwardedAttachmentInfo.type, forwardedAttachmentPart.contentType)
      }
    }
  }

  @Test
  fun testSendEncryptedMsg() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Encrypted Message",
      msg = "Encrypted Message",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = Random.nextLong()
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(3000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val encryptedContent =
          (mimeMessage.content as MimeMultipart).getBodyPart(0).content as String
        val buffer = ByteArrayOutputStream()

        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
        )

        val messageMetadata = getMessageMetadata(
          inputStream = ByteArrayInputStream(encryptedContent.toByteArray()),
          outputStream = buffer,
          pgpSecretKeyRing = pgpSecretKeyRing
        )
        assertEquals(true, messageMetadata.isEncrypted)
        assertEquals(true, messageMetadata.isSigned)
        assertEquals(outgoingMessageInfo.msg, String(buffer.toByteArray()))
      }
    }
  }

  @Test
  fun testSendEncryptedMsgWithAtt() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Encrypted Message + Att",
      msg = "Encrypted Message + Att",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = Random.nextLong(),
      atts = listOf(attachmentInfo)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(3000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
        )

        val multipart = mimeMessage.content as MimeMultipart

        //check content
        val encryptedTextContent = multipart.getBodyPart(0).content as String
        val messageOutputStream = ByteArrayOutputStream()
        val messageOpenPgpMetadata = getMessageMetadata(
          inputStream = ByteArrayInputStream(encryptedTextContent.toByteArray()),
          outputStream = messageOutputStream,
          pgpSecretKeyRing = pgpSecretKeyRing
        )

        assertEquals(true, messageOpenPgpMetadata.isEncrypted)
        assertEquals(true, messageOpenPgpMetadata.isSigned)
        assertEquals(outgoingMessageInfo.msg, String(messageOutputStream.toByteArray()))

        //check attachment
        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(ATTACHMENT_NAME + "." + Constants.PGP_FILE_EXT, attachmentPart.fileName)

        val attachmentOutputStream = ByteArrayOutputStream()
        val attachmentMessageMetadata = getMessageMetadata(
          inputStream = attachmentPart.inputStream,
          outputStream = attachmentOutputStream,
          pgpSecretKeyRing = pgpSecretKeyRing
        )

        assertEquals(ATTACHMENT_NAME, attachmentMessageMetadata.filename)
        assertEquals(true, attachmentMessageMetadata.isEncrypted)
        assertEquals(
          String(requireNotNull(attachmentInfo.rawData)),
          String(attachmentOutputStream.toByteArray())
        )
      }
    }
  }

  @Test
  fun testSendEncryptedMsgWithForwardedAtt() {
    val encryptedForwardedAttachmentInfo = prepareForwardedAttachment(
      OutgoingMessageInfo(
        account = addAccountToDatabaseRule.account.email,
        subject = "Encrypted Message + Att",
        msg = "Encrypted Message + Att",
        toRecipients = listOf(InternetAddress(addAccountToDatabaseRule.account.email)),
        from = InternetAddress(addAccountToDatabaseRule.account.email),
        encryptionType = MessageEncryptionType.ENCRYPTED,
        messageType = MessageType.NEW,
        uid = Random.nextLong(),
        atts = listOf(attachmentInfo)
      )
    )?.copy(
      name = attachmentInfo.name,
      decryptWhenForward = true,
      isForwarded = true
    )
    assertNotNull(encryptedForwardedAttachmentInfo)

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Fwd: Simple encrypted message + attachment",
      msg = "Some forwarded text",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.FORWARD,
      uid = Random.nextLong(),
      forwardedAtts = listOf(requireNotNull(encryptedForwardedAttachmentInfo))
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(5000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
        )

        val multipart = mimeMessage.content as MimeMultipart

        //check content
        val encryptedTextContent = multipart.getBodyPart(0).content as String
        val messageOutputStream = ByteArrayOutputStream()
        val messageOpenPgpMetadata = getMessageMetadata(
          inputStream = ByteArrayInputStream(encryptedTextContent.toByteArray()),
          outputStream = messageOutputStream,
          pgpSecretKeyRing = pgpSecretKeyRing
        )

        assertEquals(true, messageOpenPgpMetadata.isEncrypted)
        assertEquals(true, messageOpenPgpMetadata.isSigned)
        assertEquals(outgoingMessageInfo.msg, String(messageOutputStream.toByteArray()))

        //check attachment
        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(
          encryptedForwardedAttachmentInfo.name + "." + Constants.PGP_FILE_EXT,
          attachmentPart.fileName
        )

        //try to decrypt the forwarded attachment
        val attachmentOutputStream = ByteArrayOutputStream()
        val attachmentMessageMetadata = getMessageMetadata(
          inputStream = attachmentPart.inputStream,
          outputStream = attachmentOutputStream,
          pgpSecretKeyRing = pgpSecretKeyRing
        )

        assertEquals(encryptedForwardedAttachmentInfo.name, attachmentMessageMetadata.filename)
        assertEquals(true, attachmentMessageMetadata.isEncrypted)
      }
    }
  }

  @Test
  fun testHideKeyIdsWhenSendingEncryptedEmailWithBccRecipients() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Encrypted Message",
      msg = "Encrypted Message",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      bccRecipients = listOf(InternetAddress(bccPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = Random.nextLong()
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    runBlocking {
      delay(3000)
      checkExistingMsgOnServer(sentFolder.fullName, outgoingMessageInfo) { _, mimeMessage ->
        val encryptedContent =
          (mimeMessage.content as MimeMultipart).getBodyPart(0).content as String
        val buffer = ByteArrayOutputStream()

        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
        )

        val messageMetadata = getMessageMetadata(
          inputStream = ByteArrayInputStream(encryptedContent.toByteArray()),
          outputStream = buffer,
          pgpSecretKeyRing = pgpSecretKeyRing
        )
        assertEquals(true, messageMetadata.isEncrypted)
        assertEquals(true, messageMetadata.isSigned)
        assertEquals(outgoingMessageInfo.msg, String(buffer.toByteArray()))

        val expectedIds = arrayOf(
          PgpKey.parseKeys(recipientPgpKeyDetails.publicKey)
            .pgpKeyRingCollection
            .pgpPublicKeyRingCollection
            .first()
            .publicKeys
            .asSequence()
            .toList()[1].keyID,
          PgpKey.parseKeys(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey)
            .pgpKeyRingCollection
            .pgpPublicKeyRingCollection
            .first()
            .publicKeys
            .asSequence()
            .toList()[1].keyID,
          0,
        ).sortedArray()

        val actualIds = messageMetadata.recipientKeyIds.toTypedArray().sortedArray()

        assertArrayEquals(
          "Expected = ${expectedIds.contentToString()}, actual = ${actualIds.contentToString()}",
          expectedIds,
          actualIds
        )

        assertFalse(
          messageMetadata.recipientKeyIds.contains(
            PgpKey.parseKeys(bccPgpKeyDetails.publicKey)
              .pgpKeyRingCollection
              .pgpPublicKeyRingCollection
              .first()
              .publicKeys
              .asSequence()
              .toList()[1].keyID
          )
        )
      }
    }
  }

  private suspend fun <T> checkExistingMsgOnServer(
    folderName: String,
    outgoingMessageInfo: OutgoingMessageInfo,
    useLast: Boolean = true,
    action: suspend (folder: IMAPFolder, message: MimeMessage) -> T
  ): T =
    withContext(Dispatchers.IO) {
      val connection = IMAPStoreConnection(context, addAccountToDatabaseRule.account)
      //need to wait for email server internal sync
      delay(3000)
      connection.store.use { store ->
        connection.executeIMAPAction {
          store.getFolder(folderName).use { folder ->
            val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
            //get the message that we added to a folder recently
            val mimeMessage = imapFolder.messages.getOrElse(
              if (useLast) imapFolder.messages.lastIndex else 0
            ) { throw NoSuchElementException("List is empty.") } as MimeMessage

            val buffer = ByteArrayOutputStream()
            mimeMessage.writeTo(buffer)
            //do base checks
            assertEquals(buffer.toString(), outgoingMessageInfo.subject, mimeMessage.subject)
            assertArrayEquals(
              buffer.toString(),
              arrayOf(outgoingMessageInfo.from),
              mimeMessage.from
            )
            assertArrayEquals(
              buffer.toString(),
              outgoingMessageInfo.toRecipients?.toTypedArray(),
              mimeMessage.getRecipients(Message.RecipientType.TO)
            )
            assertArrayEquals(
              buffer.toString(),
              outgoingMessageInfo.ccRecipients?.toTypedArray(),
              mimeMessage.getRecipients(Message.RecipientType.CC)
            )
            val expectedAttachmentCount = outgoingMessageInfo.atts.orEmpty().size +
                outgoingMessageInfo.forwardedAtts.orEmpty().size

            val actualAttachmentCount = getAttCount(mimeMessage)
            assertEquals(buffer.toString(), expectedAttachmentCount, actualAttachmentCount)
            //do external checks
            action.invoke(folder, mimeMessage)
          }
        }
      }
    }

  private fun processOutgoingMessageInfo(outgoingMessageInfo: OutgoingMessageInfo) {
    //later we will replace it with Worker checking
    runBlocking {
      val worker = TestListenableWorkerBuilder<PrepareOutgoingMessagesWorker>(context).build()
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
    }
  }

  private suspend fun getAttCount(part: Part): Int = withContext(Dispatchers.IO) {
    try {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multiPart = part.content as Multipart
        val partsNumber = multiPart.count
        var attCount = 0
        for (partCount in 0 until partsNumber) {
          val bodyPart = multiPart.getBodyPart(partCount)
          if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            attCount += getAttCount(bodyPart)
          } else if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
            attCount++
          }
        }
        return@withContext attCount
      } else {
        return@withContext 0
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext 0
    }
  }

  private suspend fun deleteAllMessagesInFolder(folderName: String) =
    withContext(Dispatchers.IO) {
      val connection = IMAPStoreConnection(context, addAccountToDatabaseRule.account)
      return@withContext connection.store.use { store ->
        connection.executeIMAPAction {
          store.getFolder(folderName).use { folder ->
            val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
            val messages = imapFolder.messages
            if (messages.isNotEmpty()) {
              imapFolder.setFlags(messages, Flags(Flags.Flag.DELETED), true)
            }
          }
        }
      }
    }

  private fun getMessageMetadata(
    inputStream: InputStream,
    outputStream: ByteArrayOutputStream,
    pgpSecretKeyRing: PGPSecretKeyRing
  ) = PgpDecryptAndOrVerify.decrypt(
    srcInputStream = inputStream,
    destOutputStream = outputStream,
    secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
    protector = PasswordBasedSecretKeyRingProtector.forKey(
      pgpSecretKeyRing,
      Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    )
  )

  private fun prepareForwardedAttachment(outgoingMessageInfo: OutgoingMessageInfo): AttachmentInfo? {
    processOutgoingMessageInfo(outgoingMessageInfo)

    return runBlocking {
      val worker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(
        JavaEmailConstants.FOLDER_INBOX,
        outgoingMessageInfo
      ) { folder, mimeMessage ->
        EmailUtil.getAttsInfoFromPart(mimeMessage).map {
          it.copy(
            email = addAccountToDatabaseRule.account.email,
            folder = folder.fullName,
            uid = folder.getUID(mimeMessage)
          )
        }.firstOrNull()
      }
    }
  }

  companion object {
    const val ATTACHMENT_NAME = "test.txt"
  }
}
