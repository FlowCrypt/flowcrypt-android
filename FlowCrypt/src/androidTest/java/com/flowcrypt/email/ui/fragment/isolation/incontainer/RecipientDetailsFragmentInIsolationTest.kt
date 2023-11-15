/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.RecipientDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.RecipientDetailsFragmentArgs
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.Date

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RecipientDetailsFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val firstKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/default@flowcrypt.test_fisrtKey_pub.asc")
  private val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(
      AddRecipientsToDatabaseRule(
        listOf(
          RecipientWithPubKeys(
            RecipientEntity(
              email = addAccountToDatabaseRule.account.email,
              name = TestConstants.DEFAULT_PASSWORD
            ),
            listOf(
              firstKeyDetails.toPublicKeyEntity(addAccountToDatabaseRule.account.email)
                .copy(id = 10)
            )
          )
        )
      )
    )
    .around(ScreenshotTestRule())

  @Test
  fun testRecipientDetailsWithFullInfo() {
    launchFragmentInContainer<RecipientDetailsFragment>(
      fragmentArgs = RecipientDetailsFragmentArgs(
        recipientEntity = RecipientEntity(
          email = addAccountToDatabaseRule.account.email,
          name = TestConstants.DEFAULT_PASSWORD
        )
      ).toBundle()
    )

    onView(withId(R.id.tVName))
      .check(matches(isDisplayed()))
      .check(matches(withText(TestConstants.DEFAULT_PASSWORD)))

    onView(withId(R.id.tVEmail))
      .check(matches(isDisplayed()))
      .check(matches(withText(addAccountToDatabaseRule.account.email)))
  }

  @Test
  fun testRecipientDetailsWithEmailOnly() {
    launchFragmentInContainer<RecipientDetailsFragment>(
      fragmentArgs = RecipientDetailsFragmentArgs(
        recipientEntity = RecipientEntity(
          email = addAccountToDatabaseRule.account.email
        )
      ).toBundle()
    )

    onView(withId(R.id.tVName))
      .check(matches(isDisplayed()))
      .check(matches(withText("...")))

    onView(withId(R.id.tVEmail))
      .check(matches(isDisplayed()))
      .check(matches(withText(addAccountToDatabaseRule.account.email)))
  }

  @Test
  fun testDisplayValidPubKey() {
    launchFragmentInContainer<RecipientDetailsFragment>(
      fragmentArgs = RecipientDetailsFragmentArgs(
        recipientEntity = RecipientEntity(
          email = addAccountToDatabaseRule.account.email
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVPubKeys))
      .check(
        matches(
          hasItem(
            withChild(
              allOf(
                hasSibling(
                  allOf(
                    withId(R.id.tVPrimaryUserOrEmail),
                    withText(
                      firstKeyDetails.primaryMimeAddress?.personal ?: firstKeyDetails.primaryUserId
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.tVPrimaryUserEmail),
                    withText(firstKeyDetails.primaryMimeAddress?.address)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewManyUserIds),
                    withEffectiveVisibility(ViewMatchers.Visibility.GONE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.tVCreationDate),
                    withText(dateFormat.format(Date(firstKeyDetails.created)))
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewExpiration),
                    withText(
                      getResString(
                        R.string.key_expiration,
                        getResString(R.string.key_does_not_expire)
                      )
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.tVFingerprint),
                    withText(
                      GeneralUtil.doSectionsInText(
                        originalString = firstKeyDetails.fingerprint,
                        groupSize = 4
                      )
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewStatus),
                    withText(
                      firstKeyDetails.getStatusText(getTargetContext())
                    ),
                    withViewBackgroundTint(
                      requireNotNull(
                        firstKeyDetails.getColorStateListDependsOnStatus(
                          getTargetContext()
                        )
                      )
                    ),
                    withTextViewDrawable(
                      firstKeyDetails.getStatusIconResId(),
                      TextViewDrawableMatcher.DrawablePosition.LEFT
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewEncryptionFlag),
                    withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewSignFlag),
                    withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewAuthFlag),
                    withEffectiveVisibility(ViewMatchers.Visibility.GONE)
                  )
                )
              )
            )
          )
        )
      )
  }
}