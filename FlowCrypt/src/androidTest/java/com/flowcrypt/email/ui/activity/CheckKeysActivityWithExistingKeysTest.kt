/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.ReadyForCIAnnotation
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 02.03.2018
 * Time: 16:17
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckKeysActivityWithExistingKeysTest : BaseTest() {
  private val privateKeys = PrivateKeysManager.getKeysFromAssets(arrayOf("pgp/default@denbond7.com_fisrtKey_prv_default.asc"))

  override val activityScenarioRule = activityScenarioRule<CheckKeysActivity>(
      CheckKeysActivity.newIntent(getTargetContext(),
          privateKeys = privateKeys,
          type = KeyDetails.Type.EMAIL,
          subTitle = getTargetContext().resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              privateKeys.size, privateKeys.size),
          positiveBtnTitle = getTargetContext().getString(R.string.continue_),
          negativeBtnTitle = getTargetContext().getString(R.string.use_another_account))
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(AddPrivateKeyToDatabaseRule(
          accountEntity = addAccountToDatabaseRule.account,
          keyPath = "pgp/not_attester_user@denbond7.com_prv_default.asc",
          passphrase = TestConstants.DEFAULT_PASSWORD,
          type = KeyDetails.Type.EMAIL
      ))
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testShowMsgEmptyPassPhrase() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .perform(scrollTo(), typeText("some pass phrase"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.password_is_incorrect))
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .perform(scrollTo(), typeText(TestConstants.DEFAULT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testCheckClickButtonNegative() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonNegativeAction))
        .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == CheckKeysActivity.RESULT_NEGATIVE)
  }
}
