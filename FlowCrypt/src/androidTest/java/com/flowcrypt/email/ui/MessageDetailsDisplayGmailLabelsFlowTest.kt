/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.extensions.kotlin.capitalize
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseGmailLabelsFlowTest
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
class MessageDetailsDisplayGmailLabelsFlowTest : BaseGmailLabelsFlowTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addLabelsToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisplayLabels() {
    val details = genIncomingMessageInfo()?.msgEntity
    requireNotNull(details)
    launchActivity(details)
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewLabels))
      .check(matches(withRecyclerViewItemCount(LABELS.size + 1)))

    onView(withId(R.id.recyclerViewLabels))
      .perform(
        scrollTo<ViewHolder>(hasDescendant(withText(GmailApiHelper.LABEL_INBOX.capitalize())))
      )

    for (label in LABELS) {
      onView(withId(R.id.recyclerViewLabels))
        .perform(RecyclerViewActions.scrollToHolder(GmailApiLabelMatcher(label)))
        .check(
          matches(
            hasDescendant(
              allOf(
                isDisplayed(),
                hasDescendant(withText(label.name)),
                withViewBackgroundTint(
                  getTargetContext(),
                  requireNotNull(label.backgroundColor)
                )
              )
            )
          )
        )
    }
  }
}
