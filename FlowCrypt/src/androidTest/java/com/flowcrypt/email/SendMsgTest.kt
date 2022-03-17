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
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.util.PrivateKeysManager
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

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
        email = addAccountToDatabaseRule.account.email
      ),
      listOf(
        addPrivateKeyToDatabaseRule.pgpKeyDetails
          .toPublicKeyEntity(addAccountToDatabaseRule.account.email)
          .copy(id = 1)
      )
    ),
    RecipientWithPubKeys(
      RecipientEntity(
        email = recipientPgpKeyDetails.getUserIdsAsSingleString()
      ),
      listOf(
        recipientPgpKeyDetails
          .toPublicKeyEntity(recipientPgpKeyDetails.getUserIdsAsSingleString())
          .copy(id = 2)
      )
    )
  )

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
        assertTrue((mimeMessage.content as MimeMultipart).getBodyPart(0).content == outgoingMessageInfo.msg)
      }
    }
  }

  @Test
  fun testSendStandardMsgWithAtt() {
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = "Standard Message",
      msg = "Standard Message",
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
        assertTrue((mimeMessage.content as MimeMultipart).getBodyPart(0).content == outgoingMessageInfo.msg)
      }
    }
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
}
