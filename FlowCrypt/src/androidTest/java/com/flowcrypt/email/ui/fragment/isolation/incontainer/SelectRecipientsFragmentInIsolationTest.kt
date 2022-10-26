/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.ComposeScreenDisallowUpdateRevokedKeyFlowTest
import com.flowcrypt.email.ui.activity.fragment.SelectRecipientsFragment
import com.flowcrypt.email.ui.activity.fragment.SelectRecipientsFragmentArgs
import com.flowcrypt.email.ui.base.AddAccountToDatabaseRuleInterface
import org.hamcrest.Matchers.not
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/12/22
 *         Time: 12:56 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectRecipientsFragmentInIsolationTest : BaseTest(), AddAccountToDatabaseRuleInterface {

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  @Test
  @Ignore("failed sometimes on CI")
  fun testShowEmptyView() {
    launchFragmentInContainer<SelectRecipientsFragment>(
      fragmentArgs = SelectRecipientsFragmentArgs().toBundle()
    )
    onView(withId(R.id.tVEmpty))
      .check(matches(isDisplayed()))
      .check(matches(withText(R.string.no_results)))
  }

  @Test
  @Ignore("failed sometimes on CI")
  fun testShowNonEmptyList() {
    AddRecipientsToDatabaseRule(
      listOf(
        RecipientWithPubKeys(
          RecipientEntity(email = ComposeScreenDisallowUpdateRevokedKeyFlowTest.RECIPIENT_WITH_REVOKED_KEY),
          listOf(
            addPrivateKeyToDatabaseRule.pgpKeyDetails.toPublicKeyEntity(
              ComposeScreenDisallowUpdateRevokedKeyFlowTest.RECIPIENT_WITH_REVOKED_KEY
            ).copy(id = 12)
          )
        )
      )
    ).execute()

    launchFragmentInContainer<SelectRecipientsFragment>(
      fragmentArgs = SelectRecipientsFragmentArgs().toBundle()
    )

    onView(withId(R.id.recyclerViewContacts))
      .check(matches(not(withEmptyRecyclerView())))
  }
}
