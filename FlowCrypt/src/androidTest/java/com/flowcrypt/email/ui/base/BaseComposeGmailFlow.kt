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
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.matchers.ToolBarTitleMatcher.Companion.withText
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.OutgoingMessageConfigurationRule
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.DraftsGmailAPITestCorrectSendingFlowTest
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.not
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailFlow : BaseComposeScreenTest() {
  protected val sentCache = mutableListOf<com.google.api.services.gmail.model.Message>()
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  @get:Rule
  val outgoingMessageConfigurationRule = OutgoingMessageConfigurationRule()

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

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = listOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_DRAFT,
        folderAlias = JavaEmailConstants.FOLDER_DRAFT,
        attributes = listOf("\\HasNoChildren", "\\Draft")
      ), LocalFolder(
        account = addAccountToDatabaseRule.account.email,
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
    openComposeScreenAndFillData()
  }

  protected fun openComposeScreenAndFillData() {
    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    fillInAllFields(
      to = outgoingMessageConfiguration.to.map { requireNotNull(it.asInternetAddress()) },
      subject = outgoingMessageConfiguration.subject,
      message = outgoingMessageConfiguration.message,
    )
  }

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    when {
      request.path?.startsWith("/attester/pub", ignoreCase = true) == true -> {
        val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

        return when {
          TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(lastSegment, true) -> {
            MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
          }

          else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }

      request.path == "/v1/keys/private" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ApiHelper.getInstance(getTargetContext()).gson
            .toJson(EkmPrivateKeysResponse(privateKeys = listOf(Key(decryptedPrivateKey))))
        )
      }

      request.path == "/gmail/v1/users/me/settings/sendAs" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListSendAsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            sendAs = emptyList()
          }.toString()
        )
      }

      request.path == "/gmail/v1/users/me/labels" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
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
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListMessagesResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            messages = emptyList()
          }.toString()
        )
      }

      request.method == "POST" && request.path == "/upload/gmail/v1/users/me/messages/send?uploadType=resumable" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
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

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(message.toString())
      }

      else -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
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

  protected fun doAfterSendingChecks(
    action: (
      outgoingMessageConfiguration: OutgoingMessageConfiguration,
      message: MimeMessage
    ) -> Unit
  ) {
    //need to wait some time while the app send a message
    Thread.sleep(5000)

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
    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    //do base checks
    assertEquals(rawMime, outgoingMessageConfiguration.subject, mimeMessage.subject)
    assertArrayEquals(
      rawMime,
      arrayOf(InternetAddress(addAccountToDatabaseRule.account.email)),
      mimeMessage.from
    )
    if (outgoingMessageConfiguration.to.isNotEmpty()) {
      assertArrayEquals(
        rawMime,
        outgoingMessageConfiguration.to.mapNotNull { it.asInternetAddress() }.toTypedArray(),
        mimeMessage.getRecipients(Message.RecipientType.TO)
      )
    }
    if (outgoingMessageConfiguration.cc.isNotEmpty()) {
      assertArrayEquals(
        rawMime,
        outgoingMessageConfiguration.cc.mapNotNull { it.asInternetAddress() }.toTypedArray(),
        mimeMessage.getRecipients(Message.RecipientType.CC)
      )
    }
    if (outgoingMessageConfiguration.bcc.isNotEmpty()) {
      assertArrayEquals(
        rawMime,
        outgoingMessageConfiguration.bcc.mapNotNull { it.asInternetAddress() }.toTypedArray(),
        mimeMessage.getRecipients(Message.RecipientType.BCC)
      )
    }

    //do external checks
    action.invoke(outgoingMessageConfiguration, mimeMessage)
  }

  companion object {
    const val MESSAGE_ID_SENT = "5555555555555553"
    const val THREAD_ID_SENT = "1111111111111113"
    const val BASE_URL = "https://flowcrypt.test"
    const val LOCATION_URL =
      "/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location"

    private const val ATTACHMENTS_COUNT = 3
    var atts: MutableList<File> = mutableListOf()

    @BeforeClass
    @JvmStatic
    fun setUp() {
      createFilesForCommonAtts()
    }

    @AfterClass
    @JvmStatic
    fun tearDown() {
      TestGeneralUtil.deleteFiles(atts)
    }

    fun genFileContent(id: Int): String {
      return "$id - Text for filling the attached file"
    }

    private fun createFilesForCommonAtts() {
      for (i in 0 until ATTACHMENTS_COUNT) {
        atts.add(
          TestGeneralUtil.createFileWithTextContent(
            "$i.txt", genFileContent(i)
          )
        )
      }
    }
  }
}