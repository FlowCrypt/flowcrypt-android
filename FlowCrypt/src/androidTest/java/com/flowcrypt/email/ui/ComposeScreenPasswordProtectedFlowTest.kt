/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.clickOnChipCloseIcon
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenPasswordProtectedFlowTest : BaseComposeScreenTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val temporaryFolderRule = TemporaryFolder.builder().parentFolder(SHARED_FOLDER).build()

  override val addAccountToDatabaseRule: AddAccountToDatabaseRule
    get() = AddAccountToDatabaseRule(
      AccountDaoManager.getDefaultAccountDao().copy(useCustomerFesUrl = true)
    )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(temporaryFolderRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowWebPortalPasswordButton() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton(),
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
            resourceId = R.drawable.ic_password_not_protected_white_24,
            drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
          )
        )
      )
  }

  @Test
  fun testWebPortalPasswordButtonVisibility() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )

    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())

    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(isDisplayed()))

    onView(withId(R.id.recyclerViewChipsTo)).perform(
      actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickOnChipCloseIcon())
    )
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())
    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(not(isDisplayed())))
  }

  @Test
  fun testHideWebPortalPasswordButtonWhenUseStandardMsgType() {
    testShowWebPortalPasswordButton()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(not(isDisplayed())))
  }
}
