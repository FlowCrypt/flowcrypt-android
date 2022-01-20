/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseCreateMessageActivityTest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 1/14/22
 *         Time: 11:29 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageFragmentPasswordProtectedTest : BaseCreateMessageActivityTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val temporaryFolderRule = TemporaryFolder()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(temporaryFolderRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowWebPortalPasswordButton() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextRecipientTo))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard()
      )

    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())

    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.tap_to_protect_with_web_portal_password))))
      .check(
        matches(
          withTextViewDrawable(
            R.drawable.ic_password_not_protected_white_24,
            TextViewDrawableMatcher.DrawablePosition.LEFT
          )
        )
      )
  }

  @Test
  fun testWebPortalPasswordButtonVisibility() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextRecipientTo))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard()
      )
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextRecipientTo))
      .perform(
        clearText(), typeText("some text"), clearText(),
      )
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())
    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(not(isDisplayed())))
  }
}
