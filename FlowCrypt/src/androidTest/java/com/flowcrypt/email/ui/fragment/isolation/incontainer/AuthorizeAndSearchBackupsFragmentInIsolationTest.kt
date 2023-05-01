/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.AuthorizeAndSearchBackupsFragment
import com.flowcrypt.email.ui.activity.fragment.AuthorizeAndSearchBackupsFragmentArgs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
@DependsOnMailServer
class AuthorizeAndSearchBackupsFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Test
  fun testReturnExceptionForFailedAuthorization() {
    val requestKey = "testReturnExceptionForFailedAuthorization"
    val scenario = launchFragmentInContainer<AuthorizeAndSearchBackupsFragment>(
      fragmentArgs = AuthorizeAndSearchBackupsFragmentArgs(
        requestKey = requestKey,
        account = addAccountToDatabaseRule.account.copy(
          email = "unknown@flowcrypt.test",
          username = "unknown@flowcrypt.test"
        )
      ).toBundle()
    )

    lateinit var actualResult: Result<*>
    scenario.onFragment { fragment ->
      fragment.parentFragmentManager
        .setFragmentResultListener(
          requestKey,
          fragment
        ) { _, bundle ->
          actualResult = requireNotNull(
            bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) as? Result<*>
          )
        }
    }

    onView(withId(R.id.progress))
      .check(matches(isDisplayed()))
    assertEquals(Result.Status.EXCEPTION, actualResult.status)
  }

  @Test
  fun testSearchBackupsSuccess() {
    val requestKey = "testSearchBackupsSuccess"
    val scenario = launchFragmentInContainer<AuthorizeAndSearchBackupsFragment>(
      fragmentArgs = AuthorizeAndSearchBackupsFragmentArgs(
        requestKey = requestKey,
        account = addAccountToDatabaseRule.account
      ).toBundle()
    )

    lateinit var actualResult: Result<*>
    scenario.onFragment { fragment ->
      fragment.parentFragmentManager
        .setFragmentResultListener(
          requestKey,
          fragment
        ) { _, bundle ->
          actualResult = requireNotNull(
            bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_PRIVATE_KEY_BACKUPS_RESULT) as? Result<*>
          )
        }
    }

    onView(withId(R.id.progress))
      .check(matches(isDisplayed()))
    assertEquals(Result.Status.SUCCESS, actualResult.status)
    assertTrue((actualResult.data as ArrayList<*>).isNotEmpty())
  }
}
