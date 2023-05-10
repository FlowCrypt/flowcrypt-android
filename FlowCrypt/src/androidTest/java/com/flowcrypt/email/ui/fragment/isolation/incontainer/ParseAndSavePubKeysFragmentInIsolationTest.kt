/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.ParseAndSavePubKeysFragment
import com.flowcrypt.email.ui.activity.fragment.ParseAndSavePubKeysFragmentArgs
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.not
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
class ParseAndSavePubKeysFragmentInIsolationTest : BaseTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Test
  fun testParsingStringSourceWithUnsupportedPublicKeys() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/keys/pub_keys_5_good_1_unsupported.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(withRecyclerViewItemCount(5)))

    onView(withId(R.id.rVPubKeys))
      .check(
        matches(
          not(
            hasItem(
              withChild(
                hasSibling(
                  withText(
                    getResString(
                      R.string.template_message_part_public_key_owner,
                      "dsa@flowcrypt.test"
                    )
                  )
                )
              )
            )
          )
        )
      )
  }
}
