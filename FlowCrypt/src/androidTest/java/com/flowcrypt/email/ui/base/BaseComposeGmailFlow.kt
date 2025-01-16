/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FlowCryptMimeMessage
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.org.pgpainless.decryption_verification.isSigned
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.matchers.ToolBarTitleMatcher.Companion.withText
import com.flowcrypt.email.rules.OutgoingMessageConfigurationRule
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.MessagePartHeader
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.mail.BodyPart
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailFlow(accountEntity: AccountEntity = BASE_ACCOUNT_ENTITY) :
  BaseGmailApiTest(accountEntity) {
  protected val sentCache = mutableListOf<com.google.api.services.gmail.model.Message>()

  @get:Rule
  val outgoingMessageConfigurationRule = OutgoingMessageConfigurationRule()

  @Before
  fun prepareTest() {
    sentCache.clear()
    openComposeScreenAndFillDataIfNeeded()
  }

  open fun prepareRecipientsForTest(): List<RecipientWithPubKeys> {
    return listOf(
      RecipientWithPubKeys(
        RecipientEntity(
          email = accountEntity.email,
          name = "Default"
        ),
        listOf(
          addPrivateKeyToDatabaseRule.pgpKeyRingDetails
            .toPublicKeyEntity(accountEntity.email)
            .copy(id = 1)
        )
      )
    )
  }

  override fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    return when {
      request.method == "POST" && request.path == "/upload/gmail/v1/users/me/messages/send?uploadType=resumable" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Location", BASE_URL + LOCATION_URL)
          .setBody(com.google.api.services.gmail.model.Message().apply {
            factory = GsonFactory.getDefaultInstance()
            id = MESSAGE_ID_SENT
            threadId = THREAD_ID_SENT
            labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
          }.toString())
      }

      request.method == "PUT" && request.path == LOCATION_URL -> {
        val message = com.google.api.services.gmail.model.Message().apply {
          factory = GsonFactory.getDefaultInstance()
          id = MESSAGE_ID_SENT
          threadId = THREAD_ID_SENT
          labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
          raw = request.body.inputStream().readText()
        }

        sentCache.add(message)

        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(message.toString())
      }


      else -> super.handleCommonAPICalls(request)
    }
  }

  protected fun getMessageMetadata(
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

  protected fun checkEncryptedAttachment(
    attachmentPart: BodyPart,
    fileName: String,
    fileBytes: ByteArray
  ) {
    val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
      requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
    )
    val attachmentOutputStream = ByteArrayOutputStream()
    val attachmentMessageMetadata = getMessageMetadata(
      inputStream = attachmentPart.inputStream,
      outputStream = attachmentOutputStream,
      pgpSecretKeyRing = pgpSecretKeyRing
    )
    val decryptedBytes = attachmentOutputStream.toByteArray()
    assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
    assertEquals(fileName + "." + Constants.PGP_FILE_EXT, attachmentPart.fileName)
    assertEquals(fileName, attachmentMessageMetadata.filename)
    assertEquals(true, attachmentMessageMetadata.isEncrypted)
    assertEquals(false, attachmentMessageMetadata.isSigned)
    assertArrayEquals(fileBytes, decryptedBytes)
  }

  protected fun checkStandardAttachment(
    attachmentPart: BodyPart,
    fileName: String,
    fileBytes: ByteArray
  ) {
    assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
    assertEquals(fileName, attachmentPart.fileName)
    assertArrayEquals(fileBytes, attachmentPart.inputStream.readBytes())
  }

  protected fun checkAttachedPublicKey(publicKeyPart: BodyPart) {
    assertEquals(Part.ATTACHMENT, publicKeyPart.disposition)
    assertEquals(
      "0x${addPrivateKeyToDatabaseRule.pgpKeyRingDetails.fingerprint}.asc",
      publicKeyPart.fileName
    )
    assertEquals(
      addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey,
      String(publicKeyPart.inputStream.readBytes())
    )
  }

  protected fun extractKeyId(pgpKeyRingDetails: PgpKeyRingDetails): Long {
    return PgpKey.parseKeys(pgpKeyRingDetails.publicKey)
      .pgpKeyRingCollection
      .pgpPublicKeyRingCollection
      .first()
      .publicKeys
      .asSequence()
      .toList()[1].keyID
  }

  protected fun checkEncryptedMessagePart(
    bodyPart: BodyPart,
    expectedText: String? = null,
    expectedIds: Array<Long>? = null
  ) {
    val buffer = ByteArrayOutputStream()

    val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
      requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
    )

    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    val messageMetadata = getMessageMetadata(
      inputStream = bodyPart.inputStream,
      outputStream = buffer,
      pgpSecretKeyRing = pgpSecretKeyRing
    )
    assertEquals(true, messageMetadata.isEncrypted)
    assertEquals(true, messageMetadata.isSigned)
    assertEquals(expectedText ?: outgoingMessageConfiguration.message, String(buffer.toByteArray()))

    val finalExpectedIds = expectedIds ?: mutableListOf<Long>().apply {
      add(extractKeyId(addPrivateKeyToDatabaseRule.pgpKeyRingDetails))
      if (outgoingMessageConfiguration.to.contains(DEFAULT_TO_RECIPIENT)) {
        add(extractKeyId(defaultToPgpKeyDetails))
      }
      if (outgoingMessageConfiguration.cc.contains(DEFAULT_CC_RECIPIENT)) {
        add(extractKeyId(defaultCcPgpKeyDetails))
      }
      if (outgoingMessageConfiguration.bcc.contains(DEFAULT_BCC_RECIPIENT)) {
        add(0)
      }
    }.toTypedArray().sortedArray()

    val actualIds =
      messageMetadata.recipientKeyIds.toTypedArray().sortedArray()

    assertArrayEquals(
      "Expected = ${finalExpectedIds.contentToString()}, actual = ${actualIds.contentToString()}",
      finalExpectedIds,
      actualIds
    )

    if (outgoingMessageConfiguration.bcc.isNotEmpty()) {
      //https://github.com/FlowCrypt/flowcrypt-android/issues/2306
      assertFalse(messageMetadata.recipientKeyIds.contains(extractKeyId(defaultBccPgpKeyDetails)))
    }
  }

  protected fun compareAddresses(expected: Array<String>, actual: Array<String>) {
    assertArrayEquals(
      expected
        .map { requireNotNull(it.asInternetAddress()?.address?.lowercase()) }
        .toTypedArray()
        .sortedArray(),
      actual
        .map { requireNotNull(it.asInternetAddress()?.address?.lowercase()) }
        .toTypedArray()
        .sortedArray()
    )
  }

  protected fun getEmailAddresses(mimeMessage: MimeMessage, type: Message.RecipientType) =
    mimeMessage.getRecipients(type)
      ?.map { (it as InternetAddress).address.lowercase() }
      ?.toTypedArray() ?: emptyArray()

  protected fun doAfterSendingChecks(
    action: (
      outgoingMessageConfiguration: OutgoingMessageConfiguration,
      rawMime: String,
      message: MimeMessage
    ) -> Unit
  ) {
    //need to wait some time while the app send a message
    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    var timeToWaitForSending = outgoingMessageConfiguration.timeoutToWaitSendingInMilliseconds
    while (timeToWaitForSending > 0) {
      val countOfOutgoingMessages = runBlocking {
        roomDatabase.msgDao().getOutboxMsgsSuspend(addAccountToDatabaseRule.account.email).size
      }
      if (countOfOutgoingMessages == 0) {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))
        break
      } else {
        val step = TimeUnit.SECONDS.toMillis(1)
        timeToWaitForSending -= step
        Thread.sleep(step)
      }
    }

    val finalCountOfOutgoingMessages = runBlocking {
      roomDatabase.msgDao().getOutboxMsgsSuspend(addAccountToDatabaseRule.account.email).size
    }

    assertEquals(0, finalCountOfOutgoingMessages)

    //check that we have one message in the server cache and outbox label is not displayed
    assertEquals(1, sentCache.size)
    onView(withId(R.id.toolbar))
      .check(
        matches(
          not(
            hasDescendant(
              withText(
                getQuantityString(
                  R.plurals.outbox_msgs_count,
                  1
                )
              )
            )
          )
        )
      )

    //check sent MIME message
    val rawMime = requireNotNull(sentCache.first().raw)
    val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), rawMime.toInputStream())

    //do base checks
    if (outgoingMessageConfiguration.isNew) {
      assertEquals(rawMime, outgoingMessageConfiguration.subject, mimeMessage.subject)
    }
    assertArrayEquals(
      rawMime,
      arrayOf(InternetAddress(addAccountToDatabaseRule.account.email)),
      mimeMessage.from
    )
    if (outgoingMessageConfiguration.to.isNotEmpty()) {
      compareAddresses(
        outgoingMessageConfiguration.to,
        getEmailAddresses(mimeMessage, Message.RecipientType.TO)
      )
    }
    if (outgoingMessageConfiguration.cc.isNotEmpty()) {
      compareAddresses(
        outgoingMessageConfiguration.cc,
        getEmailAddresses(mimeMessage, Message.RecipientType.CC)
      )
    }
    if (outgoingMessageConfiguration.bcc.isNotEmpty()) {
      compareAddresses(
        outgoingMessageConfiguration.bcc,
        getEmailAddresses(mimeMessage, Message.RecipientType.BCC)
      )
    }

    //check that outgoing info was deleted
    assertEquals(
      0,
      FileAndDirectoryUtils.getFilesInDir(
        OutgoingMessagesManager.getOutgoingMessagesDirectory(getTargetContext())
      ).size
    )

    //do external checks
    action.invoke(outgoingMessageConfiguration, rawMime, mimeMessage)
  }

  protected fun openReplyScreen(buttonId: Int, subjectText: String) {
    onView(
      allOf(
        withId(buttonId),
        withParent(
          withParent(
            withParent(
              hasSibling(
                allOf(
                  withId(R.id.layoutHeader),
                  withChild(
                    allOf(
                      withId(R.id.textViewSubject),
                      ViewMatchers.withText(subjectText)
                    )
                  )
                )
              )
            )
          )
        )
      )
    ).check(matches(isDisplayed()))
      .perform(scrollTo(), click())
    Thread.sleep(TimeUnit.SECONDS.toMillis(1))
  }

  private fun preparePgpMessageWithMimeContent(): String {
    val mimeMessage = FlowCryptMimeMessage(Session.getInstance(Properties()))
    mimeMessage.subject = SUBJECT_EXISTING_PGP_MIME
    mimeMessage.setFrom(addAccountToDatabaseRule.account.email)
    mimeMessage.setRecipients(Message.RecipientType.TO, addAccountToDatabaseRule.account.email)
    mimeMessage.setContent(MimeMultipart().apply {
      addBodyPart(
        MimeBodyPart().apply {
          setText(MESSAGE_EXISTING_PGP_MIME)
        }
      )

      for ((index, attachment) in attachments.withIndex()) {
        addBodyPart(
          MimeBodyPart().apply {
            dataHandler = DataHandler(object : DataSource {
              override fun getInputStream(): InputStream = attachmentsDataCache[index].inputStream()

              override fun getOutputStream(): OutputStream? = null

              override fun getContentType(): String {
                return if (index == 2) {
                  Constants.MIME_TYPE_BINARY_DATA
                } else {
                  JavaEmailConstants.MIME_TYPE_TEXT_PLAIN
                }
              }

              override fun getName(): String = attachment.name
            })

            fileName = attachment.name
            contentID = EmailUtil.generateContentId()
          })
      }
    })

    val byteArrayOutputStream = ByteArrayOutputStream()
    mimeMessage.writeTo(byteArrayOutputStream)

    return PgpEncryptAndOrSign.encryptAndOrSignMsg(
      msg = byteArrayOutputStream.toString(),
      pubKeys = listOf(
        addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey,
        defaultFromPgpKeyDetails.publicKey,
        existingCcPgpKeyDetails.publicKey,
      ),
      prvKeys = listOf(
        requireNotNull(defaultFromPgpKeyDetails.privateKey)
      ),
      secretKeyRingProtector = secretKeyRingProtector
    )
  }

  private fun prepareMessageHeaders(
    subject: String,
    dateInMilliseconds: Long,
    boundary: String
  ) = listOf(
    MessagePartHeader().apply {
      name = "MIME-Version"
      value = "1.0"
    },
    MessagePartHeader().apply {
      name = "Date"
      value = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(Date(dateInMilliseconds))
    },
    MessagePartHeader().apply {
      name = "Message-ID"
      value = EmailUtil.generateContentId()
    },
    MessagePartHeader().apply {
      name = "Subject"
      value = subject
    },
    MessagePartHeader().apply {
      name = "From"
      value = DEFAULT_FROM_RECIPIENT
    },
    MessagePartHeader().apply {
      name = "To"
      value = EXISTING_MESSAGE_TO_RECIPIENT
    },
    MessagePartHeader().apply {
      name = "Cc"
      value = EXISTING_MESSAGE_CC_RECIPIENT
    },
    MessagePartHeader().apply {
      name = "Content-Type"
      value = "multipart/mixed; boundary=\\\"$boundary\\\""
    },
  )

  private fun openComposeScreenAndFillDataIfNeeded() {
    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    if (!outgoingMessageConfiguration.isNew) {
      return
    }

    waitForObjectWithText(SUBJECT_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(10))

    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    fillData(outgoingMessageConfiguration)

    Thread.sleep(TimeUnit.SECONDS.toMillis(1))
  }

  protected fun fillData(outgoingMessageConfiguration: OutgoingMessageConfiguration) {
    fillInAllFields(
      to = outgoingMessageConfiguration.to.map { requireNotNull(it.asInternetAddress()) },
      cc = outgoingMessageConfiguration.cc.takeIf { it.isNotEmpty() }?.map {
        requireNotNull(it.asInternetAddress())
      },
      bcc = outgoingMessageConfiguration.bcc.takeIf { it.isNotEmpty() }?.map {
        requireNotNull(it.asInternetAddress())
      },
      subject = outgoingMessageConfiguration.subject,
      message = outgoingMessageConfiguration.message
    )
  }

  companion object {
    const val MESSAGE_ID_SENT = "5555555555555553"
    const val THREAD_ID_SENT = "1111111111111113"
    const val BASE_URL = "https://flowcrypt.test"
    const val LOCATION_URL =
      "/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location"
  }
}