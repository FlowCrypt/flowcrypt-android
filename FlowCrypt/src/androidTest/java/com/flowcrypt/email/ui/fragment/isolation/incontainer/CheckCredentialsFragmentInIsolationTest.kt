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
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CheckCredentialsFragment
import com.flowcrypt.email.ui.activity.fragment.CheckCredentialsFragmentArgs
import jakarta.mail.AuthenticationFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
class CheckCredentialsFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testReturnExceptionForFailedAuthorization() {
    val actualResult = checkCredentials(
      addAccountToDatabaseRule.account.copy(
        email = "unknown@flowcrypt.test",
        username = "unknown@flowcrypt.test"
      )
    )
    assertEquals(Result.Status.EXCEPTION, actualResult.status)
    //a network error or a timeout gives the same status, so check the cause as well
    val causes = generateSequence(actualResult.exception) { it.cause }.toList()
    assertTrue(
      "Expected AuthenticationFailedException in the causes: $causes",
      causes.any { it is AuthenticationFailedException }
    )
  }

  @Test
  fun testAuthorizationSuccess() {
    val actualResult = checkCredentials(addAccountToDatabaseRule.account)
    assertEquals(Result.Status.SUCCESS, actualResult.status)
    assertEquals(true, actualResult.data as Boolean)
  }

  private fun checkCredentials(accountEntity: AccountEntity): Result<*> {
    val requestKey = UUID.randomUUID().toString()
    val scenario = launchFragmentInContainer<CheckCredentialsFragment>(
      fragmentArgs = CheckCredentialsFragmentArgs(
        accountEntity = accountEntity,
        requestKey = requestKey
      ).toBundle()
    )

    //the listener runs on the main thread, the test reads the result on its own thread
    val actualResult = AtomicReference<Result<*>?>()
    scenario.onFragment { fragment ->
      fragment.parentFragmentManager
        .setFragmentResultListener(
          requestKey,
          fragment
        ) { _, bundle ->
          actualResult.set(
            requireNotNull(
              bundle.getSerializableViaExt(
                CheckCredentialsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT
              ) as? Result<*>
            )
          )
        }
    }

    onView(withId(R.id.progress))
      .check(matches(isDisplayed()))

    //the fragment returns the result after it connects to the mail server
    waitUntil(TimeUnit.SECONDS.toMillis(60)) { actualResult.get() != null }
    return requireNotNull(actualResult.get()) { "CheckCredentialsFragment didn't return a result" }
  }
}
