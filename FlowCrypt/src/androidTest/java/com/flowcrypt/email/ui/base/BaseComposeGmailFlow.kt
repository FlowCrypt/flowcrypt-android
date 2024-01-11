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
          .setBody(
            genMessage(
              messageId = MESSAGE_ID_EXISTING_STANDARD,
              messageThreadId = THREAD_ID_EXISTING_STANDARD,
              subject = SUBJECT_EXISTING_STANDARD,
              date = DATE_EXISTING_STANDARD,
              historyIdValue = HISTORY_ID_STANDARD
            )
          )
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_STANDARD}?format=full" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            genMessage(
              messageId = MESSAGE_ID_EXISTING_STANDARD,
              messageThreadId = THREAD_ID_EXISTING_STANDARD,
              subject = SUBJECT_EXISTING_STANDARD,
              date = DATE_EXISTING_STANDARD,
              historyIdValue = HISTORY_ID_STANDARD,
              isFullFormat = true
            )
          )
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
              sizeEstimate = 6387
              internalDate = 1704963592000
              payload = MessagePart().apply {
                partId = ""
              }
            }.toString()
          )
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_EXISTING_STANDARD}?fields=raw&format=raw" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            com.google.api.services.gmail.model.Message().apply {
              factory = GsonFactory.getDefaultInstance()
              raw =
                "RGVsaXZlcmVkLVRvOiBkZW5AZmxvd2NyeXB0LmNvbQ0KUmVjZWl2ZWQ6IGJ5IDIwMDI6YTlhOmUwMTowOmIwOjI4MzplMmEwOmY3Njggd2l0aCBTTVRQIGlkIHcxY3NwMTI1MzE4NmxrYjsNCiAgICAgICAgVGh1LCAxMSBKYW4gMjAyNCAwMTowMDowNSAtMDgwMCAoUFNUKQ0KWC1SZWNlaXZlZDogYnkgMjAwMjphMDU6NjUxMjoxMTA2OmIwOjUwZTo1NWJiOmE0NTMgd2l0aCBTTVRQIGlkIGw2LTIwMDIwYTA1NjUxMjExMDYwMGIwMDUwZTU1YmJhNDUzbXI0MzY4MzFsZmcuMy4xNzA0OTYzNjA1MTA4Ow0KICAgICAgICBUaHUsIDExIEphbiAyMDI0IDAxOjAwOjA1IC0wODAwIChQU1QpDQpBUkMtU2VhbDogaT0xOyBhPXJzYS1zaGEyNTY7IHQ9MTcwNDk2MzYwNTsgY3Y9bm9uZTsNCiAgICAgICAgZD1nb29nbGUuY29tOyBzPWFyYy0yMDE2MDgxNjsNCiAgICAgICAgYj1XM0F5cW9oWHV4M0dKQmVRN1FDYkRXUFlPN2tvQ0FVSWdrV0tqbGxNMmFJTXo2aE5XbW5Oak1zUGNObGNXMTVqRFANCiAgICAgICAgIHJPSHJBLy9ncjZodGRJbTh4aGF3eWFpbDBicE1kZlFaUEY1dEZwV1ZXZUlRbDFPd1pCb3FkSjFnNEhvQ0MydGt3MEpMDQogICAgICAgICA4RjQ5aGUyRkJ2YmFodlpudzJoS2MwTC8waEFMMWd5NisreDJnRlc3SWRORXBjaTA5Mk5LbjB5YXE5VXkyQVJ0K29oOA0KICAgICAgICAgRHdBcHV6d2lhNzgwdjExQXl4MklKREhPeVNtK2hpeGhNVzhFZVdaUG1WNTJRMzQ3OXUzTHJYK3U5L3IwSDk5MnVaZkMNCiAgICAgICAgIFVCa09vNXpyalprVXBUSUFMYldJQ0o1YUEvTWNIcTFLbU0ybGdNZ2JFRlZFVGFUMGV6T1lSOW1tcllWdlpiRmxKQlRODQogICAgICAgICBDSVR3PT0NCkFSQy1NZXNzYWdlLVNpZ25hdHVyZTogaT0xOyBhPXJzYS1zaGEyNTY7IGM9cmVsYXhlZC9yZWxheGVkOyBkPWdvb2dsZS5jb207IHM9YXJjLTIwMTYwODE2Ow0KICAgICAgICBoPWNjOnRvOnN1YmplY3Q6bWVzc2FnZS1pZDpkYXRlOmZyb206bWltZS12ZXJzaW9uOmRraW0tc2lnbmF0dXJlOw0KICAgICAgICBiaD14VGVjMHA4VVY2akg5K3NidmFNTXZ5YVRyT2FXbTVGVlZGMUNuS3hQam5rPTsNCiAgICAgICAgZmg9SElTVk5jWm8wU1l1ejZMbTU0Q2w5bGdUam90WHhSL1JvRDVYQTVMT2k5Yz07DQogICAgICAgIGI9aFJMSDYwVW5icEpIb0g2M20ybThUZmVrOTVOOWxtcTRSZjlkMXVaZTlaaDg1MDI2Z1ZkNDhnQ0tMQ29kMHgyOHJEDQogICAgICAgICBWczY1NlBsTjcxZk1PNGFuUUtEMnlseFNvdm5sanh3RTBjL3MxVThqQXZNYnk1UVduUmhocHNyZVk2SVRvU044SWFBUw0KICAgICAgICAga3JzdFpxWFdONG5WOUhXMk5FR3RiUmJmaFdpL3RDMUR4cStNbUNYWWtUWFFaNDBJRVd4RkFTNXg0d1Z0ME5tV2lIaXENCiAgICAgICAgIENWT1dENmp2TzFvT0R3N09uWDVvNUR6MzQ1VUo0TDhYS0RWem1mSy8vVW1RVnBYUFNhb3F3OU5OcHo3RVJQMjJGSEMyDQogICAgICAgICBjSmNhWTZqdmNtSlhURTJObFZsc1B0S3JNdHBNem5aR0NtL3k2S1VrUXFzZDFqU1l3RUs1UjFuMVJ0TFl5QnFIUldVaQ0KICAgICAgICAgQi9kdz09DQpBUkMtQXV0aGVudGljYXRpb24tUmVzdWx0czogaT0xOyBteC5nb29nbGUuY29tOw0KICAgICAgIGRraW09cGFzcyBoZWFkZXIuaT1AZ21haWwuY29tIGhlYWRlci5zPTIwMjMwNjAxIGhlYWRlci5iPWZ4V21BNlRaOw0KICAgICAgIHNwZj1wYXNzIChnb29nbGUuY29tOiBkb21haW4gb2YgZGVuYm9uZDdAZ21haWwuY29tIGRlc2lnbmF0ZXMgMjA5Ljg1LjIyMC40MSBhcyBwZXJtaXR0ZWQgc2VuZGVyKSBzbXRwLm1haWxmcm9tPWRlbmJvbmQ3QGdtYWlsLmNvbTsNCiAgICAgICBkbWFyYz1wYXNzIChwPU5PTkUgc3A9UVVBUkFOVElORSBkaXM9Tk9ORSkgaGVhZGVyLmZyb209Z21haWwuY29tDQpSZXR1cm4tUGF0aDogPGRlbmJvbmQ3QGdtYWlsLmNvbT4NClJlY2VpdmVkOiBmcm9tIG1haWwtc29yLWY0MS5nb29nbGUuY29tIChtYWlsLXNvci1mNDEuZ29vZ2xlLmNvbS4gWzIwOS44NS4yMjAuNDFdKQ0KICAgICAgICBieSBteC5nb29nbGUuY29tIHdpdGggU01UUFMgaWQgaDIwLTIwMDIwYTA1NjUxMjA1NTQwMGIwMDUwZTU4YjY1MWRlc29yOTczNzBsZmwuMTMuMjAyNC4wMS4xMS4wMS4wMC4wNQ0KICAgICAgICBmb3IgPGRlbkBmbG93Y3J5cHQuY29tPg0KICAgICAgICAoR29vZ2xlIFRyYW5zcG9ydCBTZWN1cml0eSk7DQogICAgICAgIFRodSwgMTEgSmFuIDIwMjQgMDE6MDA6MDUgLTA4MDAgKFBTVCkNClJlY2VpdmVkLVNQRjogcGFzcyAoZ29vZ2xlLmNvbTogZG9tYWluIG9mIGRlbmJvbmQ3QGdtYWlsLmNvbSBkZXNpZ25hdGVzIDIwOS44NS4yMjAuNDEgYXMgcGVybWl0dGVkIHNlbmRlcikgY2xpZW50LWlwPTIwOS44NS4yMjAuNDE7DQpBdXRoZW50aWNhdGlvbi1SZXN1bHRzOiBteC5nb29nbGUuY29tOw0KICAgICAgIGRraW09cGFzcyBoZWFkZXIuaT1AZ21haWwuY29tIGhlYWRlci5zPTIwMjMwNjAxIGhlYWRlci5iPWZ4V21BNlRaOw0KICAgICAgIHNwZj1wYXNzIChnb29nbGUuY29tOiBkb21haW4gb2YgZGVuYm9uZDdAZ21haWwuY29tIGRlc2lnbmF0ZXMgMjA5Ljg1LjIyMC40MSBhcyBwZXJtaXR0ZWQgc2VuZGVyKSBzbXRwLm1haWxmcm9tPWRlbmJvbmQ3QGdtYWlsLmNvbTsNCiAgICAgICBkbWFyYz1wYXNzIChwPU5PTkUgc3A9UVVBUkFOVElORSBkaXM9Tk9ORSkgaGVhZGVyLmZyb209Z21haWwuY29tDQpES0lNLVNpZ25hdHVyZTogdj0xOyBhPXJzYS1zaGEyNTY7IGM9cmVsYXhlZC9yZWxheGVkOw0KICAgICAgICBkPWdtYWlsLmNvbTsgcz0yMDIzMDYwMTsgdD0xNzA0OTYzNjA0OyB4PTE3MDU1Njg0MDQ7IGRhcm49Zmxvd2NyeXB0LmNvbTsNCiAgICAgICAgaD1jYzp0bzpzdWJqZWN0Om1lc3NhZ2UtaWQ6ZGF0ZTpmcm9tOm1pbWUtdmVyc2lvbjpmcm9tOnRvOmNjOnN1YmplY3QNCiAgICAgICAgIDpkYXRlOm1lc3NhZ2UtaWQ6cmVwbHktdG87DQogICAgICAgIGJoPXhUZWMwcDhVVjZqSDkrc2J2YU1NdnlhVHJPYVdtNUZWVkYxQ25LeFBqbms9Ow0KICAgICAgICBiPWZ4V21BNlRabmVVN1JXZFhPTmZCaVRobS9ETlFBeXQ1SC9DSU9NQktZcU5xUVluSndxRWJtc2VoRS94SDF1clZ5Rg0KICAgICAgICAgV3ZrWXhxOGVNTFkwZ3MyeTVreU9pbml5WXR1UWl6b1kvMEZnd1R5UFdxSjhrSTA2YVJaTE1hRVZKcTVVTWtsMVNCaW0NCiAgICAgICAgIEs3TGpGUjlwLytNbno4dGZOSmxKUWFyUEJENTRYaEZMOGpCRlB4MkJ6L2cyc0k2N3lQdXJQcWxmK1dJaERsd3pJOWlKDQogICAgICAgICBHSm16UUlTdjFvVXEyaGMyYTYxdGhHTjlSaWFqVnd2ZEpFcFBRQUpqYnlWN3dnbEwyVHZZWVpDRmgzQk5mWlRzSDNzRw0KICAgICAgICAgYlN1dWk1eERIOHlUblpFSTlZYkQ4NWtOY2F0TVVYUi9kMllyRTlIdk0zTjBhUUhZTENnZHlwckxYTFlIV2R1R1p2VloNCiAgICAgICAgIEd0d1E9PQ0KWC1Hb29nbGUtREtJTS1TaWduYXR1cmU6IHY9MTsgYT1yc2Etc2hhMjU2OyBjPXJlbGF4ZWQvcmVsYXhlZDsNCiAgICAgICAgZD0xZTEwMC5uZXQ7IHM9MjAyMzA2MDE7IHQ9MTcwNDk2MzYwNDsgeD0xNzA1NTY4NDA0Ow0KICAgICAgICBoPWNjOnRvOnN1YmplY3Q6bWVzc2FnZS1pZDpkYXRlOmZyb206bWltZS12ZXJzaW9uOngtZ20tbWVzc2FnZS1zdGF0ZQ0KICAgICAgICAgOmZyb206dG86Y2M6c3ViamVjdDpkYXRlOm1lc3NhZ2UtaWQ6cmVwbHktdG87DQogICAgICAgIGJoPXhUZWMwcDhVVjZqSDkrc2J2YU1NdnlhVHJPYVdtNUZWVkYxQ25LeFBqbms9Ow0KICAgICAgICBiPURTc1Z0c2QvSjJZN0h2Ujh0b0xIKzBJRlBkc1BIaEcyRXZnZzU1K1lvU3AwcnZHcnhHZkdpOThFU29QdHJab2wvMA0KICAgICAgICAgdm1FRGM0NUUwYndCTGZFTS9ydjRVUU1YUzdqMHhSQlRXVVdwZ2NsQjliMmxmOVBTZTl0Wkt0czlHbVl4ajYrc1QreFINCiAgICAgICAgIDJyN3Rsd0d3MkUvMVBSNjJWQnJKVkNoZ2tFYjlKc0wzNWFzYXR5T29FdFRucVlodnlkTFdTLytEcWlINU84aWY2bzNWDQogICAgICAgICByandzZkRqWDZHZmF3QXEzZndwUXc4ZnU2NXBVSHpOYjJ3RW1xOWMwWmhzbUQycW5nK2Z0ZUdpVCtyaEdIQkpRMDR6UA0KICAgICAgICAgdVE1V09QOUhiUTA3ZTFDb1RUWmhzanEvYThGbnZmK3dKclhuMThjcXlQcXNteG5LcUdwYTlZdlN5ZXQwdTNsZVc2dFcNCiAgICAgICAgIGFLZVE9PQ0KWC1HbS1NZXNzYWdlLVN0YXRlOiBBT0p1MFl6SE5OOW93bURlSjU5UWZ4V0RmeTFOT2swdXJuS1o5YmJqSkRxYkR0KzB5cUdzUlVHMA0KCTZXWTdVeGhzUDQ0L293eHlIa0VWeTViN09sY3NKYVI4TnVYbjNndlY4Vm94DQpYLUdvb2dsZS1TbXRwLVNvdXJjZTogQUdIVCtJR3h3SGtnaUI1WkFqQ3pnQTlCUkxwMTBpYVpRdmxKd0Fuc3haTlZiY0k4U1Q3ZXFhTGRLamNMbUxhNExjYW1LbWZPODc5U2hMRnJablJiWWRnSFlOVT0NClgtUmVjZWl2ZWQ6IGJ5IDIwMDI6YWMyOjRhNzQ6MDpiMDo1MGU6YTllNDoxNWMzIHdpdGggU01UUCBpZA0KIHEyMC0yMDAyMGFjMjRhNzQwMDAwMDBiMDA1MGVhOWU0MTVjM21yMzE3Njc3bGZwLjk4LjE3MDQ5NjM2MDQxNjc7IFRodSwgMTEgSmFuDQogMjAyNCAwMTowMDowNCAtMDgwMCAoUFNUKQ0KTUlNRS1WZXJzaW9uOiAxLjANCkZyb206IERlbnlzIEJvbmRhcmVua28gPGRlbmJvbmQ3QGdtYWlsLmNvbT4NCkRhdGU6IFRodSwgMTEgSmFuIDIwMjQgMTA6NTk6NTIgKzAyMDANCk1lc3NhZ2UtSUQ6IDxDQUx2UGNpV1ZlTEI3YUs1ZVNYUkY0UDhLT3lSWnNIZS15Q0Y3NTJLY01ldDBQPUs1NEFAbWFpbC5nbWFpbC5jb20-DQpTdWJqZWN0OiBTdGFuZGFyZA0KVG86IERlbiBCb25kIDxkZW5AZmxvd2NyeXB0LmNvbT4NCkNjOiBEZW4gVGVzdCA8ZGVuYm9uZDd0ZXN0QGdtYWlsLmNvbT4NCkNvbnRlbnQtVHlwZTogbXVsdGlwYXJ0L21peGVkOyBib3VuZGFyeT0iMDAwMDAwMDAwMDAwZmJkOGM0MDYwZWE3YzU5ZCINCg0KLS0wMDAwMDAwMDAwMDBmYmQ4YzQwNjBlYTdjNTlkDQpDb250ZW50LVR5cGU6IG11bHRpcGFydC9hbHRlcm5hdGl2ZTsgYm91bmRhcnk9IjAwMDAwMDAwMDAwMGZiZDhjMjA2MGVhN2M1OWIiDQoNCi0tMDAwMDAwMDAwMDAwZmJkOGMyMDYwZWE3YzU5Yg0KQ29udGVudC1UeXBlOiB0ZXh0L3BsYWluOyBjaGFyc2V0PSJVVEYtOCINCg0KUGxhaW4gdGV4dA0KDQotLSANClJlZ2FyZHMsDQpEZW55cyBCb25kYXJlbmtvDQoNCi0tMDAwMDAwMDAwMDAwZmJkOGMyMDYwZWE3YzU5Yg0KQ29udGVudC1UeXBlOiB0ZXh0L2h0bWw7IGNoYXJzZXQ9IlVURi04Ig0KQ29udGVudC1UcmFuc2Zlci1FbmNvZGluZzogcXVvdGVkLXByaW50YWJsZQ0KDQo8ZGl2IGRpcj0zRCJsdHIiPlBsYWluIHRleHQ8YnIgY2xlYXI9M0QiYWxsIj48ZGl2Pjxicj48L2Rpdj48c3BhbiBjbGFzcz0zRCI9DQpnbWFpbF9zaWduYXR1cmVfcHJlZml4Ij4tLSA8L3NwYW4-PGJyPjxkaXYgZGlyPTNEImx0ciIgY2xhc3M9M0QiZ21haWxfc2lnbmE9DQp0dXJlIiBkYXRhLXNtYXJ0bWFpbD0zRCJnbWFpbF9zaWduYXR1cmUiPjxkaXYgZGlyPTNEImx0ciI-PGRpdj48ZGl2IGRpcj0zRCI9DQpsdHIiPlJlZ2FyZHMsPC9kaXY-PGRpdiBkaXI9M0QibHRyIj5EZW55cyBCb25kYXJlbmtvPC9kaXY-PC9kaXY-PC9kaXY-PC9kaXY9DQo-PC9kaXY-DQoNCi0tMDAwMDAwMDAwMDAwZmJkOGMyMDYwZWE3YzU5Yi0tDQotLTAwMDAwMDAwMDAwMGZiZDhjNDA2MGVhN2M1OWQNCkNvbnRlbnQtVHlwZTogdGV4dC9wbGFpbjsgY2hhcnNldD0iVVMtQVNDSUkiOyBuYW1lPSJ0ZXh0LnR4dCINCkNvbnRlbnQtRGlzcG9zaXRpb246IGF0dGFjaG1lbnQ7IGZpbGVuYW1lPSJ0ZXh0LnR4dCINCkNvbnRlbnQtVHJhbnNmZXItRW5jb2Rpbmc6IGJhc2U2NA0KQ29udGVudC1JRDogPGZfbHI4emFyNXkwPg0KWC1BdHRhY2htZW50LUlkOiBmX2xyOHphcjV5MA0KDQpMUzB0TFMwdExTMHRJR0psWjJsdWJtbHVaeUJ2WmlCemVYTjBaVzBLDQotLTAwMDAwMDAwMDAwMGZiZDhjNDA2MGVhN2M1OWQNCkNvbnRlbnQtVHlwZTogaW1hZ2UvcG5nOyBuYW1lPSJpbWFnZS5wbmciDQpDb250ZW50LURpc3Bvc2l0aW9uOiBhdHRhY2htZW50OyBmaWxlbmFtZT0iaW1hZ2UucG5nIg0KQ29udGVudC1UcmFuc2Zlci1FbmNvZGluZzogYmFzZTY0DQpDb250ZW50LUlEOiA8Zl9scjh6YXI2ODE-DQpYLUF0dGFjaG1lbnQtSWQ6IGZfbHI4emFyNjgxDQoNCmlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFEQUFBQUF3Q0FZQUFBQlhBdm1IQUFBQWVFbEVRVlI0MnUzV01RckFJQXlGWVIzdEdSMTcNCjFHNzFUallVaHlJSWdVcE55di9CbTAzMERZWUFBQUFBQVBpWldtdVU3QzNSNi9Cbmk2OGxIc09YbG5zSlR6ZGZCckg3RWwxdFJndlkNCnJaTmllSnQxVXRUR2JwMlV0YkZicHhmRHI2K1RISndseDZUa0ZRc2t5VFlwaWI4TEFBQUFBT0JqRjFpVitCUzUrRFg0QUFBQUFFbEYNClRrU3VRbUNDDQotLTAwMDAwMDAwMDAwMGZiZDhjNDA2MGVhN2M1OWQtLQ0K"
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
              data = "LS0tLS0tLS0tIGJlZ2lubmluZyBvZiBzeXN0ZW0K"
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
              data = "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAeElEQVR42u3WMQrAIAyFYR3tGR" +
                  "171G71TjYUhyIIgUpNyv_Bm030DYYAAAAAAPiZWmuU7C3R6_Bni68lHsOXlnsJTzdfBrH7El1tRgv" +
                  "YrZNieJt1UtTGbp2UtbFbpxfDr6-THJwlx6TkFQskyTYpib8LAAAAAOBjF1iV-BS5-DX4AAAAAElF" +
                  "TkSuQmCC"
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

  private fun genMessage(
    messageId: String,
    messageThreadId: String,
    subject: String,
    date: String,
    historyIdValue: BigInteger,
    isFullFormat: Boolean = false
  ) =
    com.google.api.services.gmail.model.Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = messageId
      threadId = messageThreadId
      labelIds = listOf(JavaEmailConstants.FOLDER_INBOX)
      snippet = "Plain text"
      historyId = historyIdValue
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/mixed"
        filename = ""
        headers = prepareMessageHeaders(subject, date)
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
                  setSize(47)
                  if (isFullFormat) {
                    data = "UGxhaW4gdGV4dA0KDQotLSANClJlZ2FyZHMsDQpEZW55cyBCb25kYXJlbmtvDQo="
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
                  setSize(291)
                  if (isFullFormat) {
                    data =
                      "PGRpdiBkaXI9Imx0ciI-UGxhaW4gdGV4dDxiciBjbGVhcj0iYWxsIj48ZGl2Pjxicj48L2" +
                          "Rpdj48c3BhbiBjbGFzcz0iZ21haWxfc2lnbmF0dXJlX3ByZWZpeCI-LS0gPC9zcGFuPjxicj4" +
                          "8ZGl2IGRpcj0ibHRyIiBjbGFzcz0iZ21haWxfc2lnbmF0dXJlIiBkYXRhLXNtYXJ0bWFpbD0i" +
                          "Z21haWxfc2lnbmF0dXJlIj48ZGl2IGRpcj0ibHRyIj48ZGl2PjxkaXYgZGlyPSJsdHIiPlJlZ" +
                          "2FyZHMsPC9kaXY-PGRpdiBkaXI9Imx0ciI-RGVueXMgQm9uZGFyZW5rbzwvZGl2PjwvZGl2Pj" +
                          "wvZGl2PjwvZGl2PjwvZGl2Pg0K"
                  }
                }
              }
            )
          },
          MessagePart().apply {
            partId = "1"
            mimeType = "text/plain"
            filename = "text.txt"
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "text/plain; charset=\\\"US-ASCII\\\"; name=\\\"text.txt\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Disposition"
                value = "attachment; filename=\\\"text.txt\\\""
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
              setSize(30)
            }
          },
          MessagePart().apply {
            partId = "2"
            mimeType = "image/png"
            filename = "image.png"
            headers = listOf(
              MessagePartHeader().apply {
                name = "Content-Type"
                value = "image/png; name=\\\"image.png\\\""
              },
              MessagePartHeader().apply {
                name = "Content-Disposition"
                value = "attachment; filename=\\\"image.png\\\""
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
              setSize(177)
            }
          }
        )
      }
      internalDate = 1704963592000
      sizeEstimate = 6387
    }.toString()

  private fun prepareMessageHeaders(subject: String, date: String) = listOf(
    MessagePartHeader().apply {
      name = "MIME-Version"
      value = "1.0"
    },
    MessagePartHeader().apply {
      name = "Date"
      value = date
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
      value = addAccountToDatabaseRule.account.email
    },
    MessagePartHeader().apply {
      name = "Content-Type"
      value = "multipart/mixed"
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
    )
  }

  companion object {
    const val MESSAGE_ID_EXISTING_STANDARD = "5555555555555551"
    const val THREAD_ID_EXISTING_STANDARD = "1111111111111113"
    const val DATE_EXISTING_STANDARD = "Tue, 29 Nov 2022 14:30:15 +0200"
    const val SUBJECT_EXISTING_STANDARD = "Standard"
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