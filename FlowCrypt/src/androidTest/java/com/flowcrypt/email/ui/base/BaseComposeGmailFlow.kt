/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.org.pgpainless.decryption_verification.isSigned
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.matchers.ToolBarTitleMatcher.Companion.withText
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.OutgoingMessageConfigurationRule
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.DraftsGmailAPITestCorrectSendingFlowTest
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.BatchModifyMessagesRequest
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import jakarta.activation.DataSource
import jakarta.mail.BodyPart
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.GzipSource
import okio.buffer
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.not
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import rawhttp.core.RawHttp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Properties
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailFlow : BaseComposeScreenTest() {
  private val accountEntity = AccountDaoManager.getDefaultAccountDao().copy(
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE, clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
        ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
        ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
      ),
      keyManagerUrl = "https://flowcrypt.test/",
    ), useAPI = true, useCustomerFesUrl = true
  )

  final override val addAccountToDatabaseRule: AddAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)

  protected val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  protected val sentCache = mutableListOf<com.google.api.services.gmail.model.Message>()
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  protected val toPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/attested_user@flowcrypt.test_prv_default_strong.asc")
  protected val ccPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/user_without_letters@flowcrypt.test_prv_strong.asc")
  protected val bccPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/not_attested_user@flowcrypt.test-pub.asc")

  @get:Rule
  val outgoingMessageConfigurationRule = OutgoingMessageConfigurationRule()

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = accountEntity, folders = listOf(
      LocalFolder(
        account = accountEntity.email,
        fullName = JavaEmailConstants.FOLDER_DRAFT,
        folderAlias = JavaEmailConstants.FOLDER_DRAFT,
        attributes = listOf("\\HasNoChildren", "\\Draft")
      ), LocalFolder(
        account = accountEntity.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    )
  )

  protected val decryptedPrivateKey = PgpKey.decryptKey(
    requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey),
    Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  )

  @Before
  fun prepareTest() {
    openComposeScreenAndFillDataIfNeeded()
  }

  open fun prepareRecipientsForTest(): List<RecipientWithPubKeys>{
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
      ),
      RecipientWithPubKeys(
        RecipientEntity(
          email = requireNotNull(toPgpKeyDetails.primaryMimeAddress?.address),
          name = "TO"
        ),
        listOf(
          toPgpKeyDetails
            .toPublicKeyEntity(requireNotNull(toPgpKeyDetails.primaryMimeAddress?.address))
            .copy(id = 2)
        )
      ),
      RecipientWithPubKeys(
        RecipientEntity(
          email = requireNotNull(ccPgpKeyDetails.primaryMimeAddress?.address),
          name = "CC"
        ),
        listOf(
          ccPgpKeyDetails
            .toPublicKeyEntity(requireNotNull(ccPgpKeyDetails.primaryMimeAddress?.address))
            .copy(id = 3)
        )
      ),
      RecipientWithPubKeys(
        RecipientEntity(
          email = requireNotNull(bccPgpKeyDetails.primaryMimeAddress?.address),
          name = "BCC"
        ),
        listOf(
          bccPgpKeyDetails
            .toPublicKeyEntity(requireNotNull(bccPgpKeyDetails.primaryMimeAddress?.address))
            .copy(id = 4)
        )
      )
    )
  }

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    return when {
      request.path == "/v1/keys/private" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ApiHelper.getInstance(getTargetContext()).gson
            .toJson(EkmPrivateKeysResponse(privateKeys = listOf(Key(decryptedPrivateKey))))
        )
      }

      request.path == "/gmail/v1/users/me/settings/sendAs" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListSendAsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            sendAs = emptyList()
          }.toString()
        )
      }

      request.path == "/gmail/v1/users/me/labels" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListLabelsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            labels = addLabelsToDatabaseRule.folders.map {
              Label().apply {
                id = it.fullName
                name = it.folderAlias
              }
            }
          }.toString()
        )
      }

      request.path == "/gmail/v1/users/me/messages?labelIds=${JavaEmailConstants.FOLDER_INBOX}&maxResults=45" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListMessagesResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            messages = listOf(
              com.google.api.services.gmail.model.Message().apply {
                id = MESSAGE_ID_EXISTING_STANDARD
              },
              /*com.google.api.services.gmail.model.Message().apply {
                id = MESSAGE_ID_EXISTING_ENCRYPTED
              }*/
            )
          }.toString()
        )
      }

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
          id = DraftsGmailAPITestCorrectSendingFlowTest.MESSAGE_ID_SENT
          threadId = DraftsGmailAPITestCorrectSendingFlowTest.THREAD_ID_SENT
          labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
          raw = request.body.inputStream().readText()
        }

        sentCache.add(message)

        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(message.toString())
      }

      request.method == "GET" && request.path == genPathForMessageWithSomeFilds(
        MESSAGE_ID_EXISTING_STANDARD
      ) -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(genExistingStandardMessage())
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_STANDARD}?format=full" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(genExistingStandardMessage(isFullFormat = true))
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_STANDARD}?format=minimal" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            com.google.api.services.gmail.model.Message().apply {
              factory = GsonFactory.getDefaultInstance()
              id = MESSAGE_ID_EXISTING_STANDARD
              threadId = THREAD_ID_EXISTING_STANDARD
              labelIds = listOf(JavaEmailConstants.FOLDER_INBOX)
              historyId = HISTORY_ID_STANDARD
              sizeEstimate = 0 // we don't care about this parameter
              internalDate = DATE_EXISTING_STANDARD
              payload = MessagePart().apply { partId = "" }
            }.toString()
          )
      }

      request.method == "GET" && request.path == genPathToGetAttachment(
        MESSAGE_ID_EXISTING_STANDARD,
        ATTACHMENT_FIRST_OF_EXISTING_STANDARD
      ) -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            MessagePartBody().apply {
              factory = GsonFactory.getDefaultInstance()
              data = Base64.getEncoder().encodeToString(attachmentsDataCache[0])
            }.toString()
          )
      }

      request.method == "GET" && request.path == genPathToGetAttachment(
        MESSAGE_ID_EXISTING_STANDARD,
        ATTACHMENT_SECOND_OF_EXISTING_STANDARD
      ) -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            MessagePartBody().apply {
              factory = GsonFactory.getDefaultInstance()
              data = Base64.getEncoder().encodeToString(attachmentsDataCache[2])
            }.toString()
          )
      }

      request.method == "POST" && request.path == "/gmail/v1/users/me/messages/batchModify" -> {
        val source = GzipSource(request.body)
        val batchModifyMessagesRequest = GsonFactory.getDefaultInstance().fromInputStream(
          source.buffer().inputStream(),
          BatchModifyMessagesRequest::class.java
        )

        if (batchModifyMessagesRequest.ids.contains(MESSAGE_ID_EXISTING_STANDARD)) {
          MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
        } else {
          MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }

      request.method == "POST" && request.path == "/batch" -> {
        val mimeMultipart = MimeMultipart(object : DataSource {
          override fun getInputStream(): InputStream = request.body.inputStream()

          override fun getOutputStream(): OutputStream {
            throw UnsupportedOperationException()
          }

          override fun getContentType(): String {
            return request.getHeader("Content-Type") ?: throw IllegalArgumentException()
          }

          override fun getName(): String = ""
        })

        val count = mimeMultipart.count
        val rawHttp = RawHttp()
        val responseMimeMultipart = MimeMultipart()
        for (i in 0 until count) {
          try {
            val bodyPart = mimeMultipart.getBodyPart(i)
            val rawHttpRequest = rawHttp.parseRequest(bodyPart.inputStream)
            val requestBody = if (rawHttpRequest.body.isPresent) {
              rawHttpRequest.body.get().asRawBytes().toRequestBody(
                contentType = bodyPart.contentType.toMediaTypeOrNull()
              )
            } else null

            val okhttp3Request = okhttp3.Request.Builder()
              .method(
                method = rawHttpRequest.method,
                body = requestBody
              )
              .url(rawHttpRequest.uri.toURL())
              .headers(Headers.Builder().build())
              .build()

            val response = ApiHelper.getInstance(getTargetContext())
              .retrofit.callFactory().newCall(okhttp3Request).execute()

            val stringBuilder = StringBuilder().apply {
              append(response.protocol.toString().uppercase())
              append(" ")
              append(response.code)
              append(" ")
              append(response.message)
              append("\n")

              response.headers.forEach {
                append(it.first + ": " + it.second + "\n")
              }
              append("\n")
              append(response.body?.string())
            }

            responseMimeMultipart.addBodyPart(
              MimeBodyPart(
                InternetHeaders(byteArrayOf().inputStream()).apply {
                  setHeader("Content-Type", "application/http")
                  setHeader("Content-ID", "response-${i + 1}")
                },
                stringBuilder.toString().toByteArray()
              )
            )
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }

        val outputStream = ByteArrayOutputStream()
        responseMimeMultipart.writeTo(outputStream)
        val content = String(outputStream.toByteArray())
        val boundary = (ContentType(responseMimeMultipart.contentType)).getParameter("boundary")

        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", "multipart/mixed; boundary=$boundary")
          .setBody(content)
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
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

  private fun extractKeyId(pgpKeyRingDetails: PgpKeyRingDetails): Long {
    return PgpKey.parseKeys(pgpKeyRingDetails.publicKey)
      .pgpKeyRingCollection
      .pgpPublicKeyRingCollection
      .first()
      .publicKeys
      .asSequence()
      .toList()[1].keyID
  }

  protected fun checkEncryptedMessagePart(bodyPart: BodyPart) {
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
    assertEquals(outgoingMessageConfiguration.message, String(buffer.toByteArray()))

    val expectedIds = mutableListOf<Long>().apply {
      add(extractKeyId(addPrivateKeyToDatabaseRule.pgpKeyRingDetails))
      if (outgoingMessageConfiguration.to.contains(TO_RECIPIENT)) {
        add(extractKeyId(toPgpKeyDetails))
      }
      if (outgoingMessageConfiguration.cc.contains(CC_RECIPIENT)) {
        add(extractKeyId(ccPgpKeyDetails))
      }
      if (outgoingMessageConfiguration.bcc.contains(BCC_RECIPIENT)) {
        add(0)
      }
    }.toTypedArray().sortedArray()

    val actualIds =
      messageMetadata.recipientKeyIds.toTypedArray().sortedArray()

    assertArrayEquals(
      "Expected = ${expectedIds.contentToString()}, actual = ${actualIds.contentToString()}",
      expectedIds,
      actualIds
    )

    if (outgoingMessageConfiguration.bcc.isNotEmpty()) {
      //https://github.com/FlowCrypt/flowcrypt-android/issues/2306
      assertFalse(messageMetadata.recipientKeyIds.contains(extractKeyId(bccPgpKeyDetails)))
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
    Thread.sleep(outgoingMessageConfiguration.timeoutToWaitSendingInMilliseconds)

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

    //do external checks
    action.invoke(outgoingMessageConfiguration, rawMime, mimeMessage)
  }

  private fun genPathForMessageWithSomeFilds(messageId: String) =
    "/gmail/v1/users/me/messages/$messageId?fields=" +
        "id," +
        "threadId," +
        "labelIds," +
        "snippet," +
        "sizeEstimate," +
        "historyId," +
        "internalDate," +
        "payload/partId," +
        "payload/mimeType," +
        "payload/filename," +
        "payload/headers," +
        "payload/body," +
        "payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)" +
        "&format=full"

  private fun genPathToGetAttachment(messageId: String, attachmentId: String) =
    "/gmail/v1/users/me/messages/${messageId}/attachments/${attachmentId}" +
        "?fields=data&prettyPrint=false"

  private fun genExistingStandardMessage(isFullFormat: Boolean = false) =
    com.google.api.services.gmail.model.Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = MESSAGE_ID_EXISTING_STANDARD
      threadId = THREAD_ID_EXISTING_STANDARD
      labelIds = listOf(JavaEmailConstants.FOLDER_INBOX)
      snippet = SUBJECT_EXISTING_STANDARD
      historyId = HISTORY_ID_STANDARD
      val boundary = "000000000000fbd8c4060ea7c59b"
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/mixed"
        filename = ""
        headers = prepareMessageHeaders(SUBJECT_EXISTING_STANDARD, DATE_EXISTING_STANDARD, boundary)
        body = MessagePartBody().apply {
          setSize(0)
        }
        parts = listOf(
          MessagePart().apply {
            partId = "0"
            mimeType = "multipart/alternative"
            filename = ""
            headers = listOf(MessagePartHeader().apply {
              name = "Content-Type"
              value = "multipart/alternative; boundary=\\\"000000000000fbd8c2060ea7c59b\\\""
            })
            body = MessagePartBody().apply { setSize(0) }
            parts = listOf(
              MessagePart().apply {
                partId = "0.0"
                mimeType = "text/plain"
                filename = ""
                headers = listOf(MessagePartHeader().apply {
                  name = "Content-Type"
                  value = "text/plain; charset=\\\"UTF-8\\\""
                })
                body = MessagePartBody().apply {
                  setSize(MESSAGE_EXISTING_STANDARD.length)
                  if (isFullFormat) {
                    data = Base64.getEncoder()
                      .encodeToString(MESSAGE_EXISTING_STANDARD.toByteArray())
                  }
                }
              }, MessagePart().apply {
                partId = "0.1"
                mimeType = "text/html"
                filename = ""
                headers = listOf(
                  MessagePartHeader().apply {
                    name = "Content-Type"
                    value = "text/html; charset=\\\"UTF-8\\\""
                  }, MessagePartHeader().apply {
                    name = "Content-Transfer-Encoding"
                    value = "quoted-printable"
                  })
                body = MessagePartBody().apply {
                  val html = "<div dir=\"ltr\">$MESSAGE_EXISTING_STANDARD</div>"
                  setSize(html.length)
                  if (isFullFormat) {
                    data = Base64.getEncoder().encodeToString(html.toByteArray())
                  }
                }
              }
            )
          },
          MessagePart().apply {
            partId = "1"
            mimeType = "text/plain"
            filename = ATTACHMENT_NAME_1
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "text/plain; charset=\\\"US-ASCII\\\"; name=\\\"$ATTACHMENT_NAME_1\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Disposition"
                value = "attachment; filename=\\\"$ATTACHMENT_NAME_1\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Transfer-Encoding"
                value = "base64"
              },
              MessagePartHeader().apply {
                name = "Content-ID"
                value = "f_lr8zar5y0"
              },
              MessagePartHeader().apply {
                name = "X-Attachment-Id"
                value = "f_lr8zar5y0"
              },
            )
            body = MessagePartBody().apply {
              attachmentId = ATTACHMENT_FIRST_OF_EXISTING_STANDARD
              setSize(attachmentsDataCache[0].size)
            }
          },
          MessagePart().apply {
            partId = "2"
            mimeType = "image/png"
            filename = ATTACHMENT_NAME_3
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "image/png; name=\\\"$ATTACHMENT_NAME_3\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Disposition"
                value = "attachment; filename=\\\"$ATTACHMENT_NAME_3\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Transfer-Encoding"
                value = "base64"
              },
              MessagePartHeader().apply {
                name = "Content-ID"
                value = "f_lr8zar681"
              },
              MessagePartHeader().apply {
                name = "X-Attachment-Id"
                value = "f_lr8zar681"
              },
            )
            body = MessagePartBody().apply {
              attachmentId = ATTACHMENT_SECOND_OF_EXISTING_STANDARD
              setSize(attachmentsDataCache[2].size)
            }
          }
        )
      }
      internalDate = DATE_EXISTING_STANDARD
      sizeEstimate = 0 // we don't care about this parameter
    }.toString()

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
      value = EXISTING_MESSAGE_FROM_RECIPIENT
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

    if (outgoingMessageConfiguration.timeoutBeforeMovingToComposeInMilliseconds > 0) {
      Thread.sleep(outgoingMessageConfiguration.timeoutBeforeMovingToComposeInMilliseconds)
    }

    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    fillData(outgoingMessageConfiguration)
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
      message = outgoingMessageConfiguration.message,
      isNew = outgoingMessageConfiguration.isNew
    )
  }

  companion object {
    const val EXISTING_MESSAGE_FROM_RECIPIENT = TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER
    const val EXISTING_MESSAGE_TO_RECIPIENT = "default@flowcrypt.test"
    const val EXISTING_MESSAGE_CC_RECIPIENT = "denbond7@flowcrypt.test"

    const val MESSAGE_ID_EXISTING_STANDARD = "5555555555555551"
    const val THREAD_ID_EXISTING_STANDARD = "1111111111111113"
    const val DATE_EXISTING_STANDARD = 1704963592000
    const val SUBJECT_EXISTING_STANDARD = "Standard"
    const val MESSAGE_EXISTING_STANDARD = "Standard message"
    const val ATTACHMENT_FIRST_OF_EXISTING_STANDARD = "22222222222222221"
    const val ATTACHMENT_SECOND_OF_EXISTING_STANDARD = "22222222222222222"


    const val MESSAGE_ID_EXISTING_ENCRYPTED = "5555555555555552"

    val HISTORY_ID_STANDARD = BigInteger("53163127")
    val HISTORY_ID_ENCRYPTED = BigInteger("5555555")

    const val MESSAGE_ID_SENT = "5555555555555553"
    const val THREAD_ID_SENT = "1111111111111113"
    const val BASE_URL = "https://flowcrypt.test"
    const val LOCATION_URL =
      "/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location"
    const val ATTACHMENT_NAME_1 = "text.txt"
    const val ATTACHMENT_NAME_2 = "text1.txt"
    const val ATTACHMENT_NAME_3 = "binary_key.key"

    const val TO_RECIPIENT = TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER
    const val CC_RECIPIENT = "user_without_letters@flowcrypt.test"
    const val BCC_RECIPIENT = TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER

    var attachments: MutableList<File> = mutableListOf()

    /**
     * We need to have a cache of sent files to compare data after sending
     */
    var attachmentsDataCache: MutableList<ByteArray> = mutableListOf()

    @BeforeClass
    @JvmStatic
    fun setUp() {
      createFilesForCommonAtts()
    }

    @AfterClass
    @JvmStatic
    fun tearDown() {
      TestGeneralUtil.deleteFiles(attachments)
    }

    private fun createFilesForCommonAtts() {
      attachmentsDataCache.addAll(
        listOf(
          "Text attachment 1".toByteArray(), //text data
          "Text attachment 2".toByteArray(), //text data
          Random.nextBytes(1024), //binary data 1Mb
        )
      )

      attachments.addAll(
        listOf(
          TestGeneralUtil.createFileWithContent(
            fileName = ATTACHMENT_NAME_1,
            byteArray = attachmentsDataCache[0]
          ),
          TestGeneralUtil.createFileWithContent(
            fileName = ATTACHMENT_NAME_2,
            byteArray = attachmentsDataCache[1]
          ),
          TestGeneralUtil.createFileWithContent(
            fileName = ATTACHMENT_NAME_3,
            byteArray = attachmentsDataCache[2]
          )
        )
      )
    }
  }
}