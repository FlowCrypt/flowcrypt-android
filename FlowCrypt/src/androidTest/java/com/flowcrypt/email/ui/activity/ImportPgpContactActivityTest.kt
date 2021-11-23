/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 * Date: 4/23/21
 * Time: 10:18 AM
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("fix me")
class ImportPgpContactActivityTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val useIntents: Boolean = true
  /*override val activityScenarioRule = activityScenarioRule<ImportRecipientsFromSourceActivity>(
    intent = ImportRecipientsFromSourceActivity.newIntent(
      context = getTargetContext(),
      accountEntity = addAccountToDatabaseRule.account
    )
  )*/

  private lateinit var fileWithPublicKey: File
  private lateinit var publicKey: String

  private val mockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
          val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

          when {
            TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER.equals(
              lastSegment, true
            ) -> {
              return MockResponse()
                .setStatus("HTTP/1.1 404 Not Found")
                .setBody(TestGeneralUtil.readResourceAsString("2.txt"))
            }

            TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
              lastSegment, true
            ) -> {
              return MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
            }
          }
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun createResources() {
    publicKey = TestGeneralUtil.readFileFromAssetsAsString(
      "pgp/" + TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "-pub.asc"
    )
    fileWithPublicKey = TestGeneralUtil.createFileAndFillWithContent(
      fileName = TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "_pub.asc",
      fileText = publicKey
    )
  }

  @After
  fun cleanResources() {
    TestGeneralUtil.deleteFiles(listOf(fileWithPublicKey))
  }

  @Test
  fun testFetchKeyFromAttesterForExistedUser() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        scrollTo(),
        clearText(),
        typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard()
      )
    onView(withId(R.id.iBSearchKey))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(containsString(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testFetchKeyFromAttesterForExistedUserImeAction() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        scrollTo(),
        clearText(),
        typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard(),
        pressImeActionButton()
      )

    onView(withText(containsString(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testFetchKeyFromAttesterForNotExistedUser() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        scrollTo(),
        clearText(),
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard()
      )
    onView(withId(R.id.iBSearchKey))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.layoutProgress))
      .check(matches(not((isDisplayed()))))
    //due to realization of MockWebServer I can't produce the same response.
    isToastDisplayed("API error: code = 404, message = ")
  }

  @Test
  fun testImportKeyFromFile() {
    val resultData = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(fileWithPublicKey)
    intending(
      allOf(
        hasAction(Intent.ACTION_CHOOSER),
        hasExtra(
          `is`(Intent.EXTRA_INTENT),
          allOf(
            hasAction(Intent.ACTION_OPEN_DOCUMENT),
            hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
            hasType("*/*")
          )
        )
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    onView(withId(R.id.buttonLoadFromFile))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testImportKeyFromClipboard() {
    addTextToClipboard("public key", publicKey)
    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))
      .check(matches(isDisplayed()))
  }
}
