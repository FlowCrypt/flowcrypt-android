/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ActivityResultMatchers
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.flowcrypt.email.R
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseCreateOrImportKeyActivityTest
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 16:51
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@Ignore("fix me")
class CreateOrImportKeyActivityWithKeysTest : BaseCreateOrImportKeyActivityTest() {

  override val activityTestRule =
      object : IntentsTestRule<CreateOrImportKeyActivity>(CreateOrImportKeyActivity::class.java) {
        override fun getActivityIntent(): Intent {
          return CreateOrImportKeyActivity.newIntent(getTargetContext(), AccountDaoManager.getDefaultAccountDao(), true)
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(activityTestRule)

  @Test
  fun testClickOnButtonCreateNewKey() {
    intending(allOf(hasComponent(ComponentName(getTargetContext(), CreatePrivateKeyActivity::class.java)),
        hasExtraWithKey(BasePassPhraseManagerActivity.KEY_EXTRA_ACCOUNT)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    onView(withId(R.id.buttonCreateNewKey))
        .check(matches(ViewMatchers.isDisplayed()))
        .perform(click())
    assertThat(activityTestRule.activityResult, ActivityResultMatchers
        .hasResultCode(Activity.RESULT_OK))
  }

  @Test
  fun testClickOnButtonSkipSetup() {
    onView(withId(R.id.buttonSkipSetup))
        .check(matches(ViewMatchers.isDisplayed()))
        .perform(click())
    assertThat(activityTestRule.activityResult, ActivityResultMatchers
        .hasResultCode(Activity.RESULT_OK))
  }
}
