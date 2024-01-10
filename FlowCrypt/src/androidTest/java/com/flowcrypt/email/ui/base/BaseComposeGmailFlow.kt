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
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import jakarta.mail.BodyPart
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimePart
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.not
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.Properties

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

  protected val recipientWithPubKeys = listOf(
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

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    when {
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

  protected fun checkEncryptedAttachment(
    attachmentPart: MimePart,
    originalFileName: String,
    originalFileContent: String
  ) {
    val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
      requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
    )

    assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
    assertEquals(originalFileName + "." + Constants.PGP_FILE_EXT, attachmentPart.fileName)

    val attachmentOutputStream = ByteArrayOutputStream()
    val attachmentMessageMetadata = getMessageMetadata(
      inputStream = attachmentPart.inputStream,
      outputStream = attachmentOutputStream,
      pgpSecretKeyRing = pgpSecretKeyRing
    )

    assertEquals(originalFileName, attachmentMessageMetadata.filename)
    assertEquals(true, attachmentMessageMetadata.isEncrypted)
    assertEquals(false, attachmentMessageMetadata.isSigned)
    assertEquals(originalFileContent, String(attachmentOutputStream.toByteArray()))
  }

  protected fun checkAttachedPublicKey(publicKeyPart: MimePart) {
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
    val encryptedContent = bodyPart.content as String
    val buffer = ByteArrayOutputStream()

    val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
      requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
    )

    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    val messageMetadata = getMessageMetadata(
      inputStream = ByteArrayInputStream(encryptedContent.toByteArray()),
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

    const val TO_RECIPIENT = TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER
    const val CC_RECIPIENT = "user_without_letters@flowcrypt.test"
    const val BCC_RECIPIENT = TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER

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