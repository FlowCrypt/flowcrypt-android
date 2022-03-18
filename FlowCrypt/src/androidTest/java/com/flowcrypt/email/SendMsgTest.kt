/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
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
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.util.PrivateKeysManager
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimePart

/**
 * @author Denis Bondarenko
 * Date: 01.02.2018
 * Time: 13:28
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
class SendMsgTest {
  private lateinit var context: Context
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val recipientPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/denbond7@flowcrypt.test_pub_primary.asc")

  private val SENT_FOLDER = LocalFolder(
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
        addPrivateKeyToDatabaseRule.pgpKeyDetails
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
    )
  )

  private val forwardedAttachmentInfo = AttachmentInfo().apply {
    email = addAccountToDatabaseRule.account.email
    folder = "INBOX"
    encodedSize = 950
    fwdUid = 0
    id = "\u003c481555a2-157f-4801-9e8a-c249b07d9990@flowcrypt\u003e"
    isDecrypted = false
    isEncryptionAllowed = true
    isForwarded = true
    isProtected = false
    name = "flowcrypt-backup-defaultflowcrypttest.key"
    orderNumber = 0
    path = "0/1"
    type =
      "text/plain; charset\u003dus-ascii; \r\n\tname\u003dflowcrypt-backup-defaultflowcrypttest.key"
    uid = 1
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
    .around(AddLabelsToDatabaseRule(addAccountToDatabaseRule.account, listOf(SENT_FOLDER)))

  @Test
  fun testSendStandardMsg() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Standard Message",
      msg = "Standard Message",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.NEW,
      uid = EmailUtil.genOutboxUID(context)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val worker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        assertEquals(
          outgoingMessageInfo.msg,
          (mimeMessage.content as MimeMultipart).getBodyPart(0).content
        )
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  @Test
  fun testSendStandardMsgWithAtt() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Standard Message + Att",
      msg = "Standard Message + Att",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.NEW,
      uid = EmailUtil.genOutboxUID(context),
      atts = listOf(AttachmentInfo().apply {
        val content = "Some text"
        name = "test.txt"
        encodedSize = content.length.toLong()
        rawData = content.toByteArray()
        type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN
        email = addAccountToDatabaseRule.account.email
        id = EmailUtil.generateContentId()
      })
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val worker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        assertEquals(
          outgoingMessageInfo.msg,
          (mimeMessage.content as MimeMultipart).getBodyPart(0).content
        )
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  @Test
  fun testSendStandardMsgWithForwardedAtt() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Fwd: Your FlowCrypt Backup - Standard",
      msg = "Some forwarded text",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.STANDARD,
      messageType = MessageType.FORWARD,
      uid = EmailUtil.genOutboxUID(context),
      forwardedAtts = listOf(forwardedAttachmentInfo)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val forwardedAttachmentsDownloaderWorker =
      TestListenableWorkerBuilder<ForwardedAttachmentsDownloaderWorker>(context).build()
    val messagesSenderWorker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val forwardedAttachmentsDownloaderWorkerResult = forwardedAttachmentsDownloaderWorker.doWork()
      assertThat(
        forwardedAttachmentsDownloaderWorkerResult,
        `is`(ListenableWorker.Result.success())
      )
      val messagesSenderWorkerResult = messagesSenderWorker.doWork()
      assertThat(messagesSenderWorkerResult, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        val multipart = mimeMessage.content as MimeMultipart
        assertEquals(outgoingMessageInfo.msg, multipart.getBodyPart(0).content)

        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(forwardedAttachmentInfo.name, attachmentPart.fileName)
        assertEquals(forwardedAttachmentInfo.encodedSize, attachmentPart.size.toLong())
        assertEquals(forwardedAttachmentInfo.type, attachmentPart.contentType)
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  @Test
  fun testSendEncryptedMsg() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Encrypted Message",
      msg = "Encrypted Message",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = EmailUtil.genOutboxUID(context)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val worker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        val encryptedContent =
          (mimeMessage.content as MimeMultipart).getBodyPart(0).content as String
        val buffer = ByteArrayOutputStream()

        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyDetails.privateKey)
        )

        PgpDecryptAndOrVerify.decrypt(
          srcInputStream = ByteArrayInputStream(encryptedContent.toByteArray()),
          destOutputStream = buffer,
          secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
          protector = PasswordBasedSecretKeyRingProtector.forKey(
            pgpSecretKeyRing,
            Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
          )
        )

        assertEquals(outgoingMessageInfo.msg, String(buffer.toByteArray()))
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  @Test
  fun testSendEncryptedMsgWithAtt() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    val attachmentInfo = AttachmentInfo().apply {
      val content = "Some text"
      name = "test.txt"
      encodedSize = content.length.toLong()
      rawData = content.toByteArray()
      type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN
      email = addAccountToDatabaseRule.account.email
      id = EmailUtil.generateContentId()
    }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Encrypted Message + Att",
      msg = "Encrypted Message + Att",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = EmailUtil.genOutboxUID(context),
      atts = listOf(attachmentInfo)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val worker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyDetails.privateKey)
        )

        val multipart = mimeMessage.content as MimeMultipart

        //check content
        val encryptedTextContent = multipart.getBodyPart(0).content as String

        assertEquals(outgoingMessageInfo.msg, String(ByteArrayOutputStream().apply {
          PgpDecryptAndOrVerify.decrypt(
            srcInputStream = ByteArrayInputStream(encryptedTextContent.toByteArray()),
            destOutputStream = this,
            secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
            protector = PasswordBasedSecretKeyRingProtector.forKey(
              pgpSecretKeyRing,
              Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
            )
          )
        }.toByteArray()))

        //check attachment
        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(attachmentInfo.name, attachmentPart.fileName)
        assertEquals(
          String(requireNotNull(attachmentInfo.rawData)),
          String(ByteArrayOutputStream().apply {
            PgpDecryptAndOrVerify.decrypt(
              srcInputStream = attachmentPart.inputStream,
              destOutputStream = this,
              secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
              protector = PasswordBasedSecretKeyRingProtector.forKey(
                pgpSecretKeyRing,
                Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
              )
            )
          }.toByteArray())
        )
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  @Test
  fun testSendEncryptedMsgWithForwardedAtt() {
    val countOfMsgBeforeTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }

    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Fwd: Your FlowCrypt Backup - Encrypted",
      msg = "Some forwarded text",
      toRecipients = listOf(InternetAddress(recipientPgpKeyDetails.getUserIdsAsSingleString())),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.FORWARD,
      uid = EmailUtil.genOutboxUID(context),
      forwardedAtts = listOf(forwardedAttachmentInfo)
    )

    processOutgoingMessageInfo(outgoingMessageInfo)

    val forwardedAttachmentsDownloaderWorker =
      TestListenableWorkerBuilder<ForwardedAttachmentsDownloaderWorker>(context).build()
    val messagesSenderWorker = TestListenableWorkerBuilder<MessagesSenderWorker>(context).build()
    runBlocking {
      val forwardedAttachmentsDownloaderWorkerResult = forwardedAttachmentsDownloaderWorker.doWork()
      assertThat(
        forwardedAttachmentsDownloaderWorkerResult,
        `is`(ListenableWorker.Result.success())
      )
      val messagesSenderWorkerResult = messagesSenderWorker.doWork()
      assertThat(messagesSenderWorkerResult, `is`(ListenableWorker.Result.success()))
      checkExistingMsgOnServer(SENT_FOLDER.fullName, outgoingMessageInfo) { mimeMessage ->
        val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
          requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyDetails.privateKey)
        )

        val multipart = mimeMessage.content as MimeMultipart

        //check content
        val encryptedTextContent = multipart.getBodyPart(0).content as String

        assertEquals(outgoingMessageInfo.msg, String(ByteArrayOutputStream().apply {
          PgpDecryptAndOrVerify.decrypt(
            srcInputStream = ByteArrayInputStream(encryptedTextContent.toByteArray()),
            destOutputStream = this,
            secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
            protector = PasswordBasedSecretKeyRingProtector.forKey(
              pgpSecretKeyRing,
              Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
            )
          )
        }.toByteArray()))

        //check attachment
        val attachmentPart = multipart.getBodyPart(1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(forwardedAttachmentInfo.name + ".pgp", attachmentPart.fileName)

        //try to decrypt the forwarded attachment
        assertEquals(
          true, PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
            srcInputStream = attachmentPart.inputStream,
            secretKeys = PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)),
            publicKeys = PGPPublicKeyRingCollection(listOf()),
            protector = PasswordBasedSecretKeyRingProtector.forKey(
              pgpSecretKeyRing,
              Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
            )
          ).isEncrypted
        )
      }
    }

    val countOfMsgAfterTest = runBlocking { countOfMsgsOnServer(SENT_FOLDER.fullName) }
    assertEquals(countOfMsgBeforeTest + 1, countOfMsgAfterTest)
  }

  private suspend fun checkExistingMsgOnServer(
    folderName: String,
    outgoingMessageInfo: OutgoingMessageInfo,
    action: suspend (message: MimeMessage) -> Unit
  ) =
    withContext(Dispatchers.IO) {
      val connection = IMAPStoreConnection(context, addAccountToDatabaseRule.account)
      connection.store.use { store ->
        connection.executeIMAPAction {
          store.getFolder(folderName).use { folder ->
            val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
            //get the latest message that we added to a folder recently
            val mimeMessage = imapFolder.getMessage(imapFolder.messageCount) as MimeMessage
            //do base checks
            assertEquals(outgoingMessageInfo.subject, mimeMessage.subject)
            assertArrayEquals(arrayOf(outgoingMessageInfo.from), mimeMessage.from)
            assertArrayEquals(
              outgoingMessageInfo.toRecipients.toTypedArray(),
              mimeMessage.getRecipients(Message.RecipientType.TO)
            )
            assertArrayEquals(
              outgoingMessageInfo.ccRecipients?.toTypedArray(),
              mimeMessage.getRecipients(Message.RecipientType.CC)
            )
            assertEquals(
              ((outgoingMessageInfo.atts ?: emptyList()) + (outgoingMessageInfo.forwardedAtts
                ?: emptyList())).size,
              getAttCount(mimeMessage)
            )
            //do external checks
            action.invoke(mimeMessage)
          }
        }
      }
    }

  private fun processOutgoingMessageInfo(outgoingMessageInfo: OutgoingMessageInfo) {
    //later we will replace it with Worker checking
    ProcessingOutgoingMessageInfoHelper.process(context, outgoingMessageInfo)
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

  private suspend fun countOfMsgsOnServer(folderName: String): Int = withContext(Dispatchers.IO) {
    var count = 0
    val connection = IMAPStoreConnection(context, addAccountToDatabaseRule.account)
    connection.store.use { store ->
      connection.executeIMAPAction {
        store.getFolder(folderName).use { folder ->
          count = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }.messageCount
        }
      }
    }
    return@withContext count
  }
}
