/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.junit.annotations.EnterpriseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
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
@EnterpriseTest
class ComposeScreenEnterpriseFlowTest : BaseComposeScreenTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowWarningIfFoundNotUsableKeySHA1() {
    val keyWithSHA1Algo =
      TestGeneralUtil.readFileFromAssetsAsByteArray("pgp/sha1@flowcrypt.test_pub.asc")
    val email = "sha1@flowcrypt.test"
    roomDatabase.recipientDao().insert(RecipientEntity(email = email))
    roomDatabase.pubKeyDao().insert(
      PublicKeyEntity(
        recipient = email,
        fingerprint = "5DE92AB364B3100D89FBF460241512660BDDC426",
        publicKey = keyWithSHA1Algo
      )
    )

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(to = setOf(requireNotNull(email.asInternetAddress())))

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(email),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY
            )
          )
        )
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.warning_one_of_recipients_has_not_usable_pub_key))
      .check(matches(isDisplayed()))
      .perform(click())
  }
}
