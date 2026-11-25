/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.IMAPStoreConnection
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseSignTest
import com.flowcrypt.email.util.AccountDaoManager
import jakarta.mail.Flags
import jakarta.mail.Folder
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.eclipse.angus.mail.imap.IMAPFolder
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/1984
 *
 * @author Denys Bondarenko
 */
@DependsOnMailServer
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useIntents = true, useCommonIdling = false)
class SubmitPublicKeyAfterCreationNonGoogleAccountFlowTest : BaseSignTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  private val userWithoutBackups = AccountDaoManager.getUserWithoutBackup()
  @Volatile
  private var isSubmitPubKeyCalled = false

  val mockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT,
    object : okhttp3.mockwebserver.Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
          val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

          when {
            userWithoutBackups.email.equals(lastSegment, true) -> {
              isSubmitPubKeyCalled = true
              return MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
            }
          }
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testCallSubmitPubKeyAfterKeyCreation() {
    try {
      onView(withId(R.id.buttonOtherEmailProvider))
        .check(matches(isDisplayed()))
        .perform(click())

      onView(withId(R.id.editTextEmail))
        .perform(clearText(), typeText(userWithoutBackups.email), closeSoftKeyboard())
      onView(withId(R.id.editTextPassword))
        .perform(clearText(), typeText(userWithoutBackups.password), closeSoftKeyboard())

      onView(withId(R.id.buttonTryToConnect))
        .check(matches(isDisplayed()))
        .perform(click())

      //the app checks the settings and searches for backups on the mail server first
      waitUntilViewMatches(
        viewId = R.id.buttonCreateNewKey,
        matcher = isDisplayed(),
        timeoutMs = TimeUnit.SECONDS.toMillis(30)
      )
      onView(withId(R.id.buttonCreateNewKey))
        .perform(click())

      val passphrase = UUID.randomUUID().toString() + UUID.randomUUID().toString()
      onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(replaceText(passphrase))
      //the button does nothing until the passphrase strength is evaluated in the background
      waitUntilViewMatches(
        viewId = R.id.textViewPasswordQualityInfo,
        matcher = withText(containsString(getResString(R.string.password_quality_subtext)))
      )
      onView(withId(R.id.buttonSetPassPhrase))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withId(R.id.editTextKeyPasswordSecond))
        .check(matches(isDisplayed()))
        .perform(replaceText(passphrase))
      onView(withId(R.id.buttonConfirmPassPhrases))
        .check(matches(isDisplayed()))
        .perform(click())

      //the app sends a backup of the key to the inbox first, then submits the public key
      waitUntil(TimeUnit.SECONDS.toMillis(30)) { isSubmitPubKeyCalled }
      assertTrue(isSubmitPubKeyCalled)
    } finally {
      deleteBackupEmails()
    }
  }

  /**
   * Other tests expect that [userWithoutBackups] has no backups in the inbox. The mail server
   * delivers the backup email asynchronously, so if the key was created, wait for the email
   * before deleting it.
   */
  private fun deleteBackupEmails() {
    runBlocking {
      val imapStoreConnection = IMAPStoreConnection(getTargetContext(), userWithoutBackups)
      imapStoreConnection.connect()
      imapStoreConnection.store.use { store ->
        store.getFolder(JavaEmailConstants.FOLDER_INBOX).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val findBackups = {
            imapFolder.messages.filter { it.subject == BACKUP_EMAIL_SUBJECT }.toTypedArray()
          }
          if (isSubmitPubKeyCalled) {
            waitUntil(TimeUnit.SECONDS.toMillis(30)) { findBackups().isNotEmpty() }
          }
          val backups = findBackups()
          if (backups.isNotEmpty()) {
            imapFolder.setFlags(backups, Flags(Flags.Flag.DELETED), true)
          }
          folder.expunge()
        }
      }
    }
  }

  companion object {
    private const val BACKUP_EMAIL_SUBJECT = "Your FlowCrypt Backup"
  }
}
