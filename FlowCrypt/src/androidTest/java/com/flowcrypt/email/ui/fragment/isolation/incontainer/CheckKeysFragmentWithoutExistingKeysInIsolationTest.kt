/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragmentArgs
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 11:45
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckKeysFragmentWithoutExistingKeysInIsolationTest : BaseTest() {
  private val privateKeys = PrivateKeysManager.getKeysFromAssets(
    arrayOf("pgp/default@flowcrypt.test_fisrtKey_prv_default.asc"),
    true
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<CheckKeysFragment>(
      fragmentArgs = CheckKeysFragmentArgs(
        privateKeys = privateKeys.toTypedArray(),
        positiveBtnTitle = getTargetContext().getString(R.string.continue_),
        negativeBtnTitle = getTargetContext().getString(R.string.use_another_account),
        initSubTitlePlurals = R.plurals.found_backup_of_your_account_key,
        sourceType = KeyImportDetails.SourceType.EMAIL
      ).toBundle()
    )
  }

  @Test
  fun testShowMsgEmptyWarning() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))
  }

  @Test
  fun testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), typeText("some pass phrase"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.password_is_incorrect))
  }
}
