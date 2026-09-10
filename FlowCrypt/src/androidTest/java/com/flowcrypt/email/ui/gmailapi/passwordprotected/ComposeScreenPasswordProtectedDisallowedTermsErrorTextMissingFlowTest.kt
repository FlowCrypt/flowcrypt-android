/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.passwordprotected

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
@OutgoingMessageConfiguration(
  to = [],
  cc = [],
  bcc = [],
  message = "",
  subject = "",
  isNew = true
)
class ComposeScreenPasswordProtectedDisallowedTermsErrorTextMissingFlowTest :
  BaseComposeScreenPasswordProtectedDisallowedTermsTest(
    ACCOUNT_ENTITY_WITH_MISSING_ERROR_TEXT
  ) {

  @get:Rule
  var ruleChain: TestRule =
    RuleChain.outerRule(RetryRule.DEFAULT)
      .around(ClearAppSettingsRule())
      .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule)
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(AddRecipientsToDatabaseRule(prepareRecipientsForTest()))
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testMissingOptionalPropertiesInClientConfiguration() {
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(ERROR_TEXT)).check(doesNotExist())
  }

  companion object {
    val ACCOUNT_ENTITY_WITH_MISSING_ERROR_TEXT =
      BASE_ACCOUNT_ENTITY.copy(
        clientConfiguration = BASE_ACCOUNT_ENTITY.clientConfiguration?.copy(
          disallowPasswordMessagesErrorText = null,
          disallowPasswordMessagesForTerms = TERMS
        )
      )
  }
}
