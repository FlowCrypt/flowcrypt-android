/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseCreateMessageActivityTest
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/11/21
 *         Time: 4:10 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageActivityTestPassInRamTest : BaseCreateMessageActivityTest() {
  private val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(passphraseType = KeyEntity.PassphraseType.RAM)

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowingNeedPassphraseDialog() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER)

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    val fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    val fingerprintFormatted = GeneralUtil.doSectionsInText(
      originalString = fingerprint, groupSize = 4
    )

    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_following_keys,
      1
    )
    onView(withId(R.id.tVStatusMessage))
      .check(matches(withText(tVStatusMessageText)))

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(CustomMatchers.withRecyclerViewItemCount(1)))

    onView(withText(fingerprintFormatted))
      .check(matches(isDisplayed()))
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(
      TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(200)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }
            }
          }

          return MockResponse().setResponseCode(404)
        }
      })
  }
}

