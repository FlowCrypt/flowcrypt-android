/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ActivityResultMatchers
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 11/2/19
 *         Time: 10:11 AM
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class BaseCreateOrImportKeyActivityTest : BaseTest() {

  @Test
  fun testClickOnButtonImportMyKey() {
    intending(allOf(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_TITLE),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    onView(withId(R.id.buttonImportMyKey))
        .check(matches(isDisplayed()))
        .perform(click())
    assertThat(activityTestRule?.activityResult, ActivityResultMatchers
        .hasResultCode(Activity.RESULT_OK))
  }

  @Test
  fun testClickOnButtonSelectAnotherAccount() {
    onView(withId(R.id.buttonSelectAnotherAccount))
        .check(matches(isDisplayed()))
        .perform(click())
    assertThat(activityTestRule?.activityResult,
        ActivityResultMatchers.hasResultCode(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT))
  }
}