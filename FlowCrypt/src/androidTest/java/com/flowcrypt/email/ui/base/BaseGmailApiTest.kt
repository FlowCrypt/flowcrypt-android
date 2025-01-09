/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.Constants
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FlowCryptMimeMessage
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.BatchModifyMessagesRequest
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.ListThreadsResponse
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import com.google.api.services.gmail.model.Thread
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMultipart
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.GzipSource
import okio.buffer
import org.junit.After
import org.junit.Before
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.SecretKeyRingProtector
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
abstract class BaseGmailApiTest(val accountEntity: AccountEntity = BASE_ACCOUNT_ENTITY) :
  BaseComposeScreenTest() {
  val attachments: MutableList<File> = mutableListOf()

  /**
   * We need to have a cache of sent files to compare data after sending
   */
  val attachmentsDataCache = listOf(
    "Text attachment 1".toByteArray(), //text data
    "Text attachment 2".toByteArray(), //text data
    Random.nextBytes(1024), //binary data 1Mb
  )

  /**
   * Due to specific executing(instrumentation tests and cache releasing)
   * we have to prepare temp files for every test instead of do that for a class only.
   */
  @Before
  fun createTempFiles() {
    createFilesForCommonAttachments()
  }

  @After
  fun deleteTempFiles() {
    TestGeneralUtil.deleteFiles(attachments)
  }

  final override val addAccountToDatabaseRule: AddAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)

  protected val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

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

  protected open fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    return when {
      request.path?.startsWith("/attester/pub", ignoreCase = true) == true -> {
        val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

        return when {
          lastSegment?.lowercase() in arrayOf(
            DEFAULT_FROM_RECIPIENT,
            DEFAULT_TO_RECIPIENT,
            DEFAULT_CC_RECIPIENT,
            DEFAULT_BCC_RECIPIENT,
            EXISTING_MESSAGE_CC_RECIPIENT,
          ) -> {
            MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(
                when (lastSegment?.lowercase()) {
                  DEFAULT_FROM_RECIPIENT -> defaultFromPgpKeyDetails.publicKey
                  DEFAULT_TO_RECIPIENT -> defaultToPgpKeyDetails.publicKey
                  DEFAULT_CC_RECIPIENT -> defaultCcPgpKeyDetails.publicKey
                  DEFAULT_BCC_RECIPIENT -> defaultBccPgpKeyDetails.publicKey
                  EXISTING_MESSAGE_CC_RECIPIENT -> existingCcPgpKeyDetails.publicKey
                  else -> ""
                }
              )
          }

          else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }

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
          LIST_MESSAGES_RESPONSE_ALL_MESSAGE.toString()
        )
      }

      request.path == "/gmail/v1/users/me/threads?labelIds=${JavaEmailConstants.FOLDER_INBOX}&maxResults=45" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          LIST_THREADS_RESPONSE_ALL_THREADS.toString()
        )
      }

      request.method == "GET" && request.path?.matches(
        REGEX_USER_MESSAGES_GET_WITH_FIELDS_FORMAT_FULL
      ) == true -> {
        genUserMessagesGetWithFieldsFormatFullResponse(request.path ?: "")
      }

      request.method == "GET" && request.path?.matches(REGEX_USER_MESSAGES_GET_FORMAT_FULL) == true -> {
        genUserMessagesGetFormatFullResponse(request.path ?: "")
      }

      request.method == "GET" && request.path?.matches(REGEX_USER_THREADS_GET_FORMAT_FULL) == true -> {
        genUserThreadsGetFormatFullResponse(request.path ?: "")
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

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_ENCRYPTED}?format=minimal" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            com.google.api.services.gmail.model.Message().apply {
              factory = GsonFactory.getDefaultInstance()
              id = MESSAGE_ID_EXISTING_ENCRYPTED
              threadId = THREAD_ID_EXISTING_ENCRYPTED
              labelIds = listOf(JavaEmailConstants.FOLDER_INBOX)
              historyId = HISTORY_ID_ENCRYPTED
              sizeEstimate = 0 // we don't care about this parameter
              internalDate = DATE_EXISTING_ENCRYPTED
              payload = MessagePart().apply { partId = "" }
            }.toString()
          )
      }

      request.method == "GET" && request.path in listOf(
        genPathToGetAttachment(
          messageId = MESSAGE_ID_EXISTING_STANDARD,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_STANDARD
        ),
        genPathToGetAttachment(
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_1,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_STANDARD
        ),
        genPathToGetAttachment(
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_2,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_STANDARD
        ),
        genPathToGetAttachment(
          messageId = MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_STANDARD
        ),
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

      request.method == "GET" && request.path in listOf(
        genPathToGetAttachment(
          messageId = MESSAGE_ID_EXISTING_ENCRYPTED,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_ENCRYPTED
        ),
        genPathToGetAttachment(
          messageId = MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
          attachmentId = ATTACHMENT_FIRST_OF_EXISTING_ENCRYPTED
        ),
      ) -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            MessagePartBody().apply {
              factory = GsonFactory.getDefaultInstance()
              val byteArrayOutputStream =
                prepareEncryptedFile(attachmentsDataCache[0], ATTACHMENT_NAME_1)
              data = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
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

      request.method == "GET" && request.path in listOf(
        genPathToGetAttachment(
          messageId = MESSAGE_ID_EXISTING_ENCRYPTED,
          attachmentId = ATTACHMENT_SECOND_OF_EXISTING_ENCRYPTED
        ),
        genPathToGetAttachment(
          messageId = MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
          attachmentId = ATTACHMENT_SECOND_OF_EXISTING_ENCRYPTED
        ),
      ) -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            MessagePartBody().apply {
              factory = GsonFactory.getDefaultInstance()
              val byteArrayOutputStream =
                prepareEncryptedFile(attachmentsDataCache[2], ATTACHMENT_NAME_3)
              data = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
            }.toString()
          )
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_PGP_MIME}?fields=raw&format=raw" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(
            Base64.getEncoder()
              .encodeToString(preparePgpMimeMessage(preparePgpMessageWithMimeContent()).toByteArray())
          )
      }

      request.method == "POST" && request.path == "/gmail/v1/users/me/messages/batchModify" -> {
        val source = GzipSource(request.body)
        val batchModifyMessagesRequest = GsonFactory.getDefaultInstance().fromInputStream(
          source.buffer().inputStream(),
          BatchModifyMessagesRequest::class.java
        )

        val handledIds = arrayOf(
          MESSAGE_ID_EXISTING_STANDARD,
          MESSAGE_ID_EXISTING_ENCRYPTED,
          MESSAGE_ID_EXISTING_PGP_MIME,
          MESSAGE_ID_THREAD_ONLY_STANDARD_1,
          MESSAGE_ID_THREAD_ONLY_STANDARD_2,
          MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE,
          MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
        )

        if (handledIds.any { batchModifyMessagesRequest.ids.contains(it) }) {
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

  private fun genUserMessagesGetWithFieldsFormatFullResponse(path: String): MockResponse {
    val messageId =
      REGEX_USER_MESSAGES_GET_WITH_FIELDS_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
    val baseResponse = MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)

    return when (messageId) {
      MESSAGE_ID_THREAD_ONLY_STANDARD_1 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_1,
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_THREAD_ONLY_STANDARD_2 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_2,
          subject = "Re: $SUBJECT_EXISTING_STANDARD",
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_THREAD_ONLY_ENCRYPTED_1 -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_ONLY_ENCRYPTED,
          messageId = MESSAGE_ID_THREAD_ONLY_ENCRYPTED_1
        ).toString()
      )

      MESSAGE_ID_THREAD_ONLY_ENCRYPTED_2 -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_ONLY_ENCRYPTED,
          messageId = MESSAGE_ID_THREAD_ONLY_ENCRYPTED_2,
          subject = "Re: $SUBJECT_EXISTING_ENCRYPTED",
        ).toString()
      )

      MESSAGE_ID_THREAD_NO_ATTACHMENTS_1 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_NO_ATTACHMENTS,
          messageId = MESSAGE_ID_THREAD_NO_ATTACHMENTS_1,
          subject = SUBJECT_NO_ATTACHMENTS,
          includeAttachments = false
        ).toString()
      )

      MESSAGE_ID_THREAD_NO_ATTACHMENTS_2 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_NO_ATTACHMENTS,
          messageId = MESSAGE_ID_THREAD_NO_ATTACHMENTS_2,
          subject = "Re: $SUBJECT_NO_ATTACHMENTS",
          includeAttachments = false
        ).toString()
      )

      MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_SINGLE_STANDARD_MESSAGE,
          messageId = MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE,
          subject = SUBJECT_SINGLE_STANDARD,
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_SINGLE_ENCRYPTED_MESSAGE,
          messageId = MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
          subject = SUBJECT_SINGLE_ENCRYPTED
        ).toString()
      )

      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_1,
      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_2,
      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_3 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT,
          messageId = messageId,
          subject = when (messageId) {
            MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_1 -> SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT
            else -> "Re: $SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT"
          },
          labels = when (messageId) {
            MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_3 -> listOf(JavaEmailConstants.FOLDER_DRAFT)
            else -> listOf(JavaEmailConstants.FOLDER_INBOX)
          },
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_1,
      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_2,
      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_3,
      MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_4 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS,
          messageId = messageId,
          subject = when (messageId) {
            MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_1 -> SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT
            else -> "Re: $SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS"
          },
          labels = when (messageId) {
            MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_3,
            MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_4 -> listOf(JavaEmailConstants.FOLDER_DRAFT)

            else -> listOf(JavaEmailConstants.FOLDER_INBOX)
          },
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_1,
      MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_2,
      MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_3 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS,
          messageId = messageId,
          subject = when (messageId) {
            MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_1 -> SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS
            else -> "Re: $SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS"
          },
          labels = when (messageId) {
            MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_2,
            MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_3 -> listOf(JavaEmailConstants.FOLDER_DRAFT)

            else -> listOf(JavaEmailConstants.FOLDER_INBOX)
          },
          includeBinaryAttachment = false
        ).toString()
      )

      MESSAGE_ID_EXISTING_STANDARD -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_EXISTING_STANDARD,
          messageId = MESSAGE_ID_EXISTING_STANDARD
        ).toString()
      )

      MESSAGE_ID_EXISTING_ENCRYPTED -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_EXISTING_ENCRYPTED,
          messageId = MESSAGE_ID_EXISTING_ENCRYPTED
        ).toString()
      )

      MESSAGE_ID_EXISTING_PGP_MIME -> baseResponse.setBody(genPGPMimeMessage())

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun genUserMessagesGetFormatFullResponse(path: String): MockResponse {
    val messageId = REGEX_USER_MESSAGES_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
    val baseResponse = MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)

    return when (messageId) {
      MESSAGE_ID_EXISTING_STANDARD -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_EXISTING_STANDARD,
          messageId = MESSAGE_ID_EXISTING_STANDARD,
          isFullFormat = true,
        ).toString()
      )

      MESSAGE_ID_EXISTING_ENCRYPTED -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_EXISTING_ENCRYPTED,
          messageId = MESSAGE_ID_EXISTING_ENCRYPTED,
          isFullFormat = true
        ).toString()
      )

      MESSAGE_ID_EXISTING_PGP_MIME -> baseResponse.setBody(
        genPGPMimeMessage(isFullFormat = true)
      )

      MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_SINGLE_STANDARD_MESSAGE,
          messageId = MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE,
          subject = SUBJECT_SINGLE_STANDARD,
          includeBinaryAttachment = false,
          isFullFormat = true
        ).toString()
      )

      MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE -> baseResponse.setBody(
        genEncryptedMessage(
          threadId = THREAD_ID_SINGLE_ENCRYPTED_MESSAGE,
          messageId = MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
          subject = SUBJECT_SINGLE_ENCRYPTED,
          isFullFormat = true
        ).toString()
      )

      MESSAGE_ID_THREAD_ONLY_STANDARD_1 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_1,
          includeBinaryAttachment = false,
          isFullFormat = true
        ).toString()
      )

      MESSAGE_ID_THREAD_ONLY_STANDARD_2 -> baseResponse.setBody(
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_2,
          subject = "Re: $SUBJECT_EXISTING_STANDARD",
          includeBinaryAttachment = false,
          isFullFormat = true
        ).toString()
      )

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun genUserThreadsGetFormatFullResponse(path: String): MockResponse {
    val threadId = REGEX_USER_THREADS_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
    val baseResponse = MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)

    return when (threadId) {
      THREAD_ID_ONLY_STANDARD -> baseResponse.setBody(
        genThreadWithOnlyStandardMessages()
      )

      THREAD_ID_ONLY_ENCRYPTED -> baseResponse.setBody(
        genThreadWithOnlyEncryptedMessages()
      )

      THREAD_ID_NO_ATTACHMENTS -> baseResponse.setBody(
        genThreadWithNoAttachments()
      )

      THREAD_ID_SINGLE_STANDARD_MESSAGE -> baseResponse.setBody(
        genThreadWithSingleStandardMessage()
      )

      THREAD_ID_SINGLE_ENCRYPTED_MESSAGE -> baseResponse.setBody(
        genThreadWithSingleEncryptedMessage()
      )

      THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT -> baseResponse.setBody(
        genThreadWithFewMessagesAndSingleDraft()
      )

      THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS -> baseResponse.setBody(
        genThreadWithFewMessagesFewDrafts()
      )

      THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS -> baseResponse.setBody(
        genThreadWithOneMessageFewDrafts()
      )

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun genPathForMessageWithSomeFields(messageId: String) =
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

  private fun genThreadWithOnlyStandardMessages() =
    Thread().apply {
      factory = GsonFactory.getDefaultInstance()
      id = THREAD_ID_ONLY_STANDARD
      messages = listOf(
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_1,
          isFullFormat = true,
          includeBinaryAttachment = false
        ),
        genStandardMessage(
          threadId = THREAD_ID_ONLY_STANDARD,
          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_2,
          subject = "Re: $SUBJECT_EXISTING_STANDARD",
          isFullFormat = true,
          includeBinaryAttachment = false
        )
      )
    }.toString()

  private fun genThreadWithOnlyEncryptedMessages() =
    Thread().apply {
      factory = GsonFactory.getDefaultInstance()
      id = THREAD_ID_ONLY_ENCRYPTED
      messages = listOf(
        genEncryptedMessage(
          threadId = THREAD_ID_ONLY_ENCRYPTED,
          messageId = MESSAGE_ID_THREAD_ONLY_ENCRYPTED_1,
          isFullFormat = true
        ),
        genEncryptedMessage(
          threadId = THREAD_ID_ONLY_ENCRYPTED,
          messageId = MESSAGE_ID_THREAD_ONLY_ENCRYPTED_2,
          subject = "Re: $SUBJECT_EXISTING_ENCRYPTED",
          isFullFormat = true
        )
      )
    }.toString()

  private fun genThreadWithNoAttachments() =
    Thread().apply {
      factory = GsonFactory.getDefaultInstance()
      id = THREAD_ID_NO_ATTACHMENTS
      messages = listOf(
        genStandardMessage(
          threadId = THREAD_ID_NO_ATTACHMENTS,
          messageId = MESSAGE_ID_THREAD_NO_ATTACHMENTS_1,
          subject = SUBJECT_NO_ATTACHMENTS,
          isFullFormat = true,
          includeAttachments = false
        ),
        genStandardMessage(
          threadId = THREAD_ID_NO_ATTACHMENTS,
          messageId = MESSAGE_ID_THREAD_NO_ATTACHMENTS_2,
          subject = "Re: $SUBJECT_NO_ATTACHMENTS",
          isFullFormat = true,
          includeAttachments = false
        )
      )
    }.toString()

  private fun genThreadWithSingleStandardMessage() = Thread().apply {
    factory = GsonFactory.getDefaultInstance()
    id = THREAD_ID_SINGLE_STANDARD_MESSAGE
    messages = listOf(
      genStandardMessage(
        threadId = THREAD_ID_SINGLE_STANDARD_MESSAGE,
        messageId = MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE,
        subject = SUBJECT_SINGLE_STANDARD,
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
    )
  }.toString()

  private fun genThreadWithSingleEncryptedMessage() = Thread().apply {
    factory = GsonFactory.getDefaultInstance()
    id = THREAD_ID_SINGLE_ENCRYPTED_MESSAGE
    messages = listOf(
      genEncryptedMessage(
        threadId = THREAD_ID_SINGLE_ENCRYPTED_MESSAGE,
        messageId = MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE,
        subject = SUBJECT_SINGLE_ENCRYPTED,
        isFullFormat = true
      ),
    )
  }.toString()

  private fun genThreadWithFewMessagesAndSingleDraft() = Thread().apply {
    factory = GsonFactory.getDefaultInstance()
    id = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT
    messages = listOf(
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_1,
        subject = SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT,
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_2,
        subject = "Re: $SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT",
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_3,
        subject = "Re: $SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT",
        includeBinaryAttachment = false,
        isFullFormat = true,
        labels = listOf(JavaEmailConstants.FOLDER_DRAFT)
      ),
    )
  }.toString()

  private fun genThreadWithFewMessagesFewDrafts() = Thread().apply {
    factory = GsonFactory.getDefaultInstance()
    id = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS
    messages = listOf(
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_1,
        subject = SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS,
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_2,
        subject = "Re: $SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS",
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_3,
        subject = "Re: $SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS",
        includeBinaryAttachment = false,
        isFullFormat = true,
        labels = listOf(JavaEmailConstants.FOLDER_DRAFT)
      ),
      genStandardMessage(
        threadId = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_4,
        subject = "Re: $SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS",
        includeBinaryAttachment = false,
        isFullFormat = true,
        labels = listOf(JavaEmailConstants.FOLDER_DRAFT)
      ),
    )
  }.toString()

  private fun genThreadWithOneMessageFewDrafts() = Thread().apply {
    factory = GsonFactory.getDefaultInstance()
    id = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS
    messages = listOf(
      genStandardMessage(
        threadId = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_1,
        subject = SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS,
        includeBinaryAttachment = false,
        isFullFormat = true
      ),
      genStandardMessage(
        threadId = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_2,
        subject = "Re: $SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS",
        includeBinaryAttachment = false,
        isFullFormat = true,
        labels = listOf(JavaEmailConstants.FOLDER_DRAFT)
      ),
      genStandardMessage(
        threadId = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS,
        messageId = MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_3,
        subject = "Re: $SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS",
        includeBinaryAttachment = false,
        isFullFormat = true,
        labels = listOf(JavaEmailConstants.FOLDER_DRAFT)
      ),
    )
  }.toString()

  private fun genStandardMessage(
    threadId: String,
    messageId: String,
    labels: List<String> = listOf(JavaEmailConstants.FOLDER_INBOX),
    subject: String = SUBJECT_EXISTING_STANDARD,
    isFullFormat: Boolean = false,
    includeAttachments: Boolean = true,
    includeBinaryAttachment: Boolean = true
  ) =
    com.google.api.services.gmail.model.Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = messageId
      this.threadId = threadId
      labelIds = labels
      snippet = subject
      historyId = HISTORY_ID_STANDARD
      val boundary = "000000000000fbd8c4060ea7c59b"
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/mixed"
        filename = ""
        headers = prepareMessageHeaders(subject, DATE_EXISTING_STANDARD, boundary)
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
            parts = mutableListOf(
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
        ).toMutableList().apply {
          if (includeAttachments) {
            add(
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
              }
            )

            if (includeBinaryAttachment) {
              add(
                MessagePart().apply {
                  partId = "2"
                  mimeType = "application/pgp-keys"
                  filename = ATTACHMENT_NAME_3
                  headers = listOf(
                    MessagePartHeader().apply {
                      name = "Content-Type"
                      value = "application/pgp-keys; name=\\\"$ATTACHMENT_NAME_3\\\""
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
          }
        }
      }
      internalDate = DATE_EXISTING_STANDARD
      sizeEstimate = 0 // we don't care about this parameter
    }

  private fun genEncryptedMessage(
    threadId: String,
    messageId: String,
    labels: List<String> = listOf(JavaEmailConstants.FOLDER_INBOX),
    subject: String = SUBJECT_EXISTING_ENCRYPTED,
    isFullFormat: Boolean = false
  ) = com.google.api.services.gmail.model.Message().apply {
      factory = GsonFactory.getDefaultInstance()
    id = messageId
    this.threadId = threadId
    labelIds = labels
    snippet = subject
      historyId = HISTORY_ID_ENCRYPTED
      val boundary = "000000000000fbd8c4060ea7c69b"
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/mixed"
        filename = ""
        headers =
          prepareMessageHeaders(subject, DATE_EXISTING_ENCRYPTED, boundary)
        body = MessagePartBody().apply {
          setSize(0)
        }
        parts = listOf(
          MessagePart().apply {
            partId = "0"
            mimeType = "text/plain"
            filename = ""
            headers = listOf(MessagePartHeader().apply {
              name = "Content-Type"
              value = "text/plain; charset=\\\"UTF-8\\\""
            })
            body = MessagePartBody().apply {
              val encryptedMsg = PgpEncryptAndOrSign.encryptAndOrSignMsg(
                msg = MESSAGE_EXISTING_ENCRYPTED,
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
              setSize(encryptedMsg.length)
              if (isFullFormat) {
                data = Base64.getEncoder().encodeToString(encryptedMsg.toByteArray())
              }
            }
          },
          MessagePart().apply {
            partId = "1"
            mimeType = "application/octet-stream"
            val fileName = ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT
            filename = fileName
            headers = generateHeadersForEncryptedAttachment(fileName, "f_lr9zar5y0")
            body = MessagePartBody().apply {
              attachmentId = ATTACHMENT_FIRST_OF_EXISTING_ENCRYPTED
              val byteArrayOutputStream =
                prepareEncryptedFile(attachmentsDataCache[0], ATTACHMENT_NAME_1)

              setSize(byteArrayOutputStream.toByteArray().size)
            }
          },
          MessagePart().apply {
            partId = "2"
            mimeType = "application/octet-stream"
            val fileName = ATTACHMENT_NAME_3 + "." + Constants.PGP_FILE_EXT
            filename = fileName
            headers = generateHeadersForEncryptedAttachment(fileName, "f_lr9zar681")
            body = MessagePartBody().apply {
              attachmentId = ATTACHMENT_SECOND_OF_EXISTING_ENCRYPTED
              val byteArrayOutputStream =
                prepareEncryptedFile(attachmentsDataCache[2], ATTACHMENT_NAME_3)

              setSize(byteArrayOutputStream.toByteArray().size)
            }
          }
        )
      }
      internalDate = DATE_EXISTING_ENCRYPTED
      sizeEstimate = 0 // we don't care about this parameter
  }

  private fun generateHeadersForEncryptedAttachment(fileName: String, id: String) = listOf(
    MessagePartHeader().apply {
      name = "Content-Type"
      value = "application/octet-stream; name=\\\"$fileName\\\""
    },
    MessagePartHeader().apply {
      name = "Content-Disposition"
      value = "attachment; filename=\\\"$fileName\\\""
    },
    MessagePartHeader().apply {
      name = "Content-Transfer-Encoding"
      value = "base64"
    },
    MessagePartHeader().apply {
      name = "Content-ID"
      value = id
    },
    MessagePartHeader().apply {
      name = "X-Attachment-Id"
      value = id
    },
  )

  private fun prepareEncryptedFile(src: ByteArray, fileName: String): ByteArrayOutputStream {
    val byteArrayOutputStream = ByteArrayOutputStream()
    PgpEncryptAndOrSign.encryptAndOrSign(
      srcInputStream = src.inputStream(),
      destOutputStream = byteArrayOutputStream,
      pubKeys = listOf(
        addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey,
        defaultFromPgpKeyDetails.publicKey,
        existingCcPgpKeyDetails.publicKey,
      ),
      fileName = fileName,
    )
    return byteArrayOutputStream
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

  private fun genPGPMimeMessage(isFullFormat: Boolean = false) =
    com.google.api.services.gmail.model.Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = MESSAGE_ID_EXISTING_PGP_MIME
      threadId = THREAD_ID_EXISTING_PGP_MIME
      labelIds = listOf(JavaEmailConstants.FOLDER_INBOX)
      snippet = SUBJECT_EXISTING_PGP_MIME
      historyId = HISTORY_ID_PGP_MIME
      val boundary = "000000000000fbd8c4060ea7c69b"
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/encrypted"
        filename = ""
        headers = prepareMessageHeaders(
          SUBJECT_EXISTING_PGP_MIME,
          DATE_EXISTING_PGP_MIME,
          boundary
        ).filterNot {
          it.name == "Content-Type"
        }.toMutableList().apply {
          MessagePartHeader().apply {
            name = "Content-Type"
            value = "multipart/encrypted;\n" +
                " protocol=\"application/pgp-encrypted\";\n" +
                " boundary=\\\"$boundary\\\""
          }
        }
        body = MessagePartBody().apply {
          setSize(0)
        }
        parts = listOf(
          MessagePart().apply {
            partId = "0"
            mimeType = "application/pgp-encrypted"
            filename = ""
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "application/pgp-encrypted"
              },
              MessagePartHeader().apply {
                name = "Content-Description"
                value = "PGP/MIME version identification"
              }
            )
            body = MessagePartBody().apply {
              val versionMessage = "Version: 1"
              setSize(versionMessage.length)
              if (isFullFormat) {
                data = Base64.getEncoder().encodeToString(versionMessage.toByteArray())
              }
            }
          },
          MessagePart().apply {
            partId = "1"
            mimeType = "application/octet-stream"
            val fileName = ATTACHMENT_PGP_MIME
            filename = fileName
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "application/octet-stream; name=\\\"$fileName\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Description"
                value = "OpenPGP encrypted message"
              },
              MessagePartHeader().apply {
                name = "Content-Disposition"
                value = "inline; filename=\\\"$fileName\\\""
              },
            )
            body = MessagePartBody().apply {
              val pgpMessageWithMimeContent = preparePgpMessageWithMimeContent()
              setSize(pgpMessageWithMimeContent.length)
            }
          }
        )
      }
      internalDate = DATE_EXISTING_PGP_MIME
      sizeEstimate = 0 // we don't care about this parameter
    }.toString()

  private fun preparePgpMimeMessage(pgpMessage: String): String {
    return "Return-Path: <default@flowcrypt.test>\n" +
        "Delivered-To: default@flowcrypt.test\n" +
        "Message-ID: <0af3b089-d018-42ba-b897-c1553caae9d5@flowcrypt.test>\n" +
        "Date: Thu, 7 Mar 2024 18:00:18 +0200\n" +
        "Mime-Version: 1.0\n" +
        "Content-Language: en-US\n" +
        "To: default@flowcrypt.test\n" +
        "From: Default User <default@flowcrypt.test>\n" +
        "Subject: ...\n" +
        "Content-Type: multipart/encrypted;\n" +
        " protocol=\"application/pgp-encrypted\";\n" +
        " boundary=\"------------vjUmb0D80S09zqu10qP9Vv0s\"\n" +
        "\n" +
        "This is an OpenPGP/MIME encrypted message (RFC 4880 and 3156)\n" +
        "--------------vjUmb0D80S09zqu10qP9Vv0s\n" +
        "Content-Type: application/pgp-encrypted\n" +
        "Content-Description: PGP/MIME version identification\n" +
        "\n" +
        "Version: 1\n" +
        "\n" +
        "--------------vjUmb0D80S09zqu10qP9Vv0s\n" +
        "Content-Type: application/octet-stream; name=\"encrypted.asc\"\n" +
        "Content-Description: OpenPGP encrypted message\n" +
        "Content-Disposition: inline; filename=\"encrypted.asc\"\n" +
        "\n" +
        pgpMessage +
        "\n" +
        "\n" +
        "--------------vjUmb0D80S09zqu10qP9Vv0s--\n"
  }

  private fun createFilesForCommonAttachments() {
    attachments.clear()
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

  companion object {
    const val EXISTING_MESSAGE_TO_RECIPIENT = "default@flowcrypt.test"
    const val EXISTING_MESSAGE_CC_RECIPIENT = "android@flowcrypt.test"

    const val POSITION_EXISTING_ENCRYPTED = 0
    const val POSITION_EXISTING_STANDARD = 1
    const val POSITION_EXISTING_PGP_MIME = 2

    const val THREAD_ID_ONLY_STANDARD = "200000e222d6c001"
    const val MESSAGE_ID_THREAD_ONLY_STANDARD_1 = "5555555559991001"
    const val MESSAGE_ID_THREAD_ONLY_STANDARD_2 = "5555555559991002"
    const val THREAD_ID_ONLY_ENCRYPTED = "200000e222d6c002"
    const val MESSAGE_ID_THREAD_ONLY_ENCRYPTED_1 = "5555555559992001"
    const val MESSAGE_ID_THREAD_ONLY_ENCRYPTED_2 = "5555555559992002"
    const val THREAD_ID_STANDARD_AND_ENCRYPTED = "200000e222d6c003"
    const val THREAD_ID_NO_ATTACHMENTS = "200000e222d6c004"
    const val MESSAGE_ID_THREAD_NO_ATTACHMENTS_1 = "5555555559993001"
    const val MESSAGE_ID_THREAD_NO_ATTACHMENTS_2 = "5555555559993002"
    const val THREAD_ID_SINGLE_STANDARD_MESSAGE = "200000e222d6c005"
    const val MESSAGE_ID_THREAD_SINGLE_STANDARD_MESSAGE = "5555555559995001"
    const val THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT = "200000e222d6c006"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_1 = "5555555559996001"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_2 = "5555555559996002"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_SINGLE_DRAFT_3 = "5555555559996003"
    const val THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS = "200000e222d6c007"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_1 = "5555555559997001"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_2 = "5555555559997002"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_3 = "5555555559997003"
    const val MESSAGE_ID_THREAD_FEW_MESSAGES_WITH_FEW_DRAFTS_4 = "5555555559997004"
    const val THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS = "200000e222d6c008"
    const val MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_1 = "5555555559998001"
    const val MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_2 = "5555555559998002"
    const val MESSAGE_ID_THREAD_ONE_MESSAGE_WITH_FEW_DRAFTS_3 = "5555555559998003"
    const val THREAD_ID_SINGLE_ENCRYPTED_MESSAGE = "200000e222d6c009"
    const val MESSAGE_ID_THREAD_SINGLE_ENCRYPTED_MESSAGE = "5555555559999001"

    const val SUBJECT_NO_ATTACHMENTS = "No attachments"
    const val SUBJECT_SINGLE_STANDARD = "Single standard message"
    const val SUBJECT_SINGLE_ENCRYPTED = "Single encrypted message"
    const val SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT = "few messages in thread + 1 draft"
    const val SUBJECT_FEW_MESSAGES_WITH_FEW_DRAFTS = "few messages in thread + few drafts"
    const val SUBJECT_ONE_MESSAGE_WITH_FEW_DRAFTS = "one message in thread + few drafts"

    const val MESSAGE_ID_EXISTING_STANDARD = "5555555555555551"
    const val THREAD_ID_EXISTING_STANDARD = "1111111111111111"
    const val DATE_EXISTING_STANDARD = 1704963592000
    const val SUBJECT_EXISTING_STANDARD = "Standard"
    const val MESSAGE_EXISTING_STANDARD = "Standard message"
    const val ATTACHMENT_FIRST_OF_EXISTING_STANDARD = "22222222222222221"
    const val ATTACHMENT_SECOND_OF_EXISTING_STANDARD = "22222222222222222"

    const val MESSAGE_ID_EXISTING_ENCRYPTED = "5555555555555552"
    const val THREAD_ID_EXISTING_ENCRYPTED = "1111111111111112"
    const val DATE_EXISTING_ENCRYPTED = 1704963599000
    const val SUBJECT_EXISTING_ENCRYPTED = "Encrypted"
    const val MESSAGE_EXISTING_ENCRYPTED = "Encrypted message"
    const val ATTACHMENT_FIRST_OF_EXISTING_ENCRYPTED = "22222222222222223"
    const val ATTACHMENT_SECOND_OF_EXISTING_ENCRYPTED = "22222222222222224"

    const val MESSAGE_ID_EXISTING_PGP_MIME = "5555555555555553"
    const val THREAD_ID_EXISTING_PGP_MIME = "1111111111111113"
    const val DATE_EXISTING_PGP_MIME = 1704963581000
    const val SUBJECT_EXISTING_PGP_MIME = "PGP/MIME Encrypted"
    const val MESSAGE_EXISTING_PGP_MIME = "PGP/MIME"

    val HISTORY_ID_STANDARD = BigInteger("53163127")
    val HISTORY_ID_ENCRYPTED = BigInteger("53163327")
    val HISTORY_ID_PGP_MIME = BigInteger("53163027")

    val REGEX_USER_MESSAGES_GET_WITH_FIELDS_FORMAT_FULL =
      ("/gmail/v1/users/me/messages/(.{16})\\?fields=id," +
          "threadId,labelIds,snippet,sizeEstimate,historyId,internalDate,payload/partId," +
          "payload/mimeType,payload/filename,payload/headers,payload/body," +
          "payload/parts\\(partId,mimeType,filename,headers," +
          "body/size,body/attachmentId\\)&format=full").toRegex()
    val REGEX_USER_MESSAGES_GET_FORMAT_FULL =
      ("/gmail/v1/users/me/messages/(.{16})\\?format=full").toRegex()
    val REGEX_USER_THREADS_GET_FORMAT_FULL =
      ("/gmail/v1/users/me/threads/(.{16})\\?format=full").toRegex()

    const val ATTACHMENT_NAME_1 = "text.txt"
    const val ATTACHMENT_NAME_2 = "text1.txt"
    const val ATTACHMENT_NAME_3 = "binary_key.key"
    const val ATTACHMENT_PGP_MIME = "encrypted.asc"

    const val DEFAULT_FROM_RECIPIENT = "From <default_from@flowcrypt.test>"
    const val DEFAULT_TO_RECIPIENT = "To <default_to@flowcrypt.test>"
    const val DEFAULT_CC_RECIPIENT = "Cc <default_cc@flowcrypt.test>"
    const val DEFAULT_BCC_RECIPIENT = "Bcc <default_bcc@flowcrypt.test>"

    val existingCcPgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
      EXISTING_MESSAGE_CC_RECIPIENT,
      TestConstants.DEFAULT_PASSWORD
    ).toPgpKeyRingDetails()
    val defaultFromPgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
      DEFAULT_FROM_RECIPIENT,
      TestConstants.DEFAULT_PASSWORD
    ).toPgpKeyRingDetails()
    val defaultToPgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
      DEFAULT_TO_RECIPIENT,
      TestConstants.DEFAULT_PASSWORD
    ).toPgpKeyRingDetails()
    val defaultCcPgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
      DEFAULT_CC_RECIPIENT,
      TestConstants.DEFAULT_PASSWORD
    ).toPgpKeyRingDetails()
    val defaultBccPgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
      DEFAULT_BCC_RECIPIENT,
      TestConstants.DEFAULT_PASSWORD
    ).toPgpKeyRingDetails()

    val secretKeyRingProtector = SecretKeyRingProtector.unlockAnyKeyWith(
      Passphrase.fromPassword(TestConstants.DEFAULT_PASSWORD)
    )

    val LIST_MESSAGES_RESPONSE_ALL_MESSAGE = ListMessagesResponse().apply {
      factory = GsonFactory.getDefaultInstance()
      messages = listOf(
        com.google.api.services.gmail.model.Message().apply {
          id = MESSAGE_ID_EXISTING_STANDARD
        },
        com.google.api.services.gmail.model.Message().apply {
          id = MESSAGE_ID_EXISTING_ENCRYPTED
        },
        com.google.api.services.gmail.model.Message().apply {
          id = MESSAGE_ID_EXISTING_PGP_MIME
        }
      )
    }

    val LIST_THREADS_RESPONSE_ALL_THREADS = ListThreadsResponse().apply {
      factory = GsonFactory.getDefaultInstance()
      threads = listOf(
        Thread().apply { id = THREAD_ID_ONLY_STANDARD },
        Thread().apply { id = THREAD_ID_ONLY_ENCRYPTED },
        Thread().apply { id = THREAD_ID_NO_ATTACHMENTS },
        Thread().apply { id = THREAD_ID_SINGLE_STANDARD_MESSAGE },
        Thread().apply { id = THREAD_ID_SINGLE_ENCRYPTED_MESSAGE },
        Thread().apply { id = THREAD_ID_FEW_MESSAGES_WITH_SINGLE_DRAFT },
        Thread().apply { id = THREAD_ID_FEW_MESSAGES_WITH_FEW_DRAFTS },
        Thread().apply { id = THREAD_ID_ONE_MESSAGE_WITH_FEW_DRAFTS },
        /*com.google.api.services.gmail.model.Thread().apply {
          id = THREAD_ID_EXISTING_ONLY_ENCRYPTED
        },*/
      )
    }

    val BASE_ACCOUNT_ENTITY = AccountDaoManager.getDefaultAccountDao().copy(
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
  }
}