/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTintResId
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.PrivateKeysListFragment
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class PrivateKeysListFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowValidKey() {
    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = pgpKeyDetails,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<PrivateKeysListFragment>()

    onView(withId(R.id.emptyView))
      .check(matches(not(isDisplayed())))

    onView(withId(R.id.recyclerViewKeys))
      .check(matches(isDisplayed()))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    doBaseCheckingForKey(
      keyOwner = "default@flowcrypt.test",
      fingerprint = "3DEBE9F677D5B9BB38E5A244225F8023C20D0957",
      creationDate = dateFormat.format(Date(pgpKeyDetails.created)),
      expirationDate = pgpKeyDetails.expiration?.let {
        getResString(R.string.key_expiration, dateFormat.format(Date(it)))
      } ?: getResString(
        R.string.key_expiration,
        getResString(R.string.key_does_not_expire)
      ),
      isUsableForEncryption = pgpKeyDetails.usableForEncryption
    )
  }

  @Test
  fun testShowRevokedKey() {
    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_secondKey_prv_strong_revoked.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = pgpKeyDetails,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<PrivateKeysListFragment>()

    onView(withId(R.id.emptyView))
      .check(matches(not(isDisplayed())))

    onView(withId(R.id.recyclerViewKeys))
      .check(matches(isDisplayed()))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    doBaseCheckingForKey(
      keyOwner = "default@flowcrypt.test",
      fingerprint = "45F0A5260A80F238598DD081C669001D0A6DCAC8",
      creationDate = dateFormat.format(Date(pgpKeyDetails.created)),
      expirationDate = pgpKeyDetails.expiration?.let {
        getResString(R.string.key_expiration, dateFormat.format(Date(it)))
      } ?: getResString(
        R.string.key_expiration,
        getResString(R.string.key_does_not_expire)
      ),
      isUsableForEncryption = pgpKeyDetails.usableForEncryption,
      statusLabelText = getResString(R.string.revoked),
      statusLabelTextColorResId = R.color.white,
      statusLabelIconResId = R.drawable.ic_outline_warning_amber_16,
      statusLabelTintColorResId = R.color.red
    )
  }

  @Test
  fun testShowExpiredKey() {
    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/expired@flowcrypt.test_prv_default.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = pgpKeyDetails,
      passphrase = TestConstants.DEFAULT_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<PrivateKeysListFragment>()

    onView(withId(R.id.emptyView))
      .check(matches(not(isDisplayed())))

    onView(withId(R.id.recyclerViewKeys))
      .check(matches(isDisplayed()))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    assertNotNull(pgpKeyDetails.expiration)
    val expiration = requireNotNull(pgpKeyDetails.expiration)
    val expectedExpirationDate = "Jan 1, 2011"
    val actualExpirationDate = dateFormat.format(Date(expiration))
    assertEquals(expectedExpirationDate, actualExpirationDate)

    doBaseCheckingForKey(
      keyOwner = "expired@flowcrypt.test",
      fingerprint = "599132F15A04487AA6356C7F717B789F05D874DA",
      creationDate = dateFormat.format(Date(pgpKeyDetails.created)),
      expirationDate = getResString(R.string.key_expiration, actualExpirationDate),
      isUsableForEncryption = pgpKeyDetails.usableForEncryption,
      statusLabelText = getResString(R.string.expired),
      statusLabelTextColorResId = R.color.white,
      statusLabelIconResId = R.drawable.ic_outline_warning_amber_16,
      statusLabelTintColorResId = R.color.orange
    )
  }

  private fun doBaseCheckingForKey(
    keyOwner: String,
    fingerprint: String,
    creationDate: String,
    expirationDate: String,
    isUsableForEncryption: Boolean,
    statusLabelText: String? = null,
    statusLabelTextColorResId: Int? = null,
    statusLabelIconResId: Int? = null,
    statusLabelTintColorResId: Int? = null
  ) {
    onView(withId(R.id.recyclerViewKeys))
      .perform(scrollToPosition<RecyclerView.ViewHolder>(1))
      .check(
        matches(
          hasDescendant(
            allOf(
              withId(R.id.textViewKeyOwner),
              withText(keyOwner)
            )
          )
        )
      )
      .check(
        matches(
          hasDescendant(
            allOf(
              withId(R.id.textViewFingerprint),
              withText(GeneralUtil.doSectionsInText(originalString = fingerprint, groupSize = 4))
            )
          )
        )
      )
      .check(
        matches(
          hasDescendant(
            allOf(
              withId(R.id.textViewExpiration),
              withText(expirationDate)
            )
          )
        )
      )
      .check(
        matches(
          hasDescendant(
            allOf(
              withId(R.id.textViewCreationDate),
              withText(creationDate)
            )
          )
        )
      )
      .check(
        matches(
          hasDescendant(
            allOf(
              withId(R.id.textViewStatus),
              if (isUsableForEncryption) not(isDisplayed()) else allOf(
                isDisplayed(),
                withText(statusLabelText),
                statusLabelTextColorResId?.let { hasTextColor(it) },
                statusLabelIconResId?.let {
                  withTextViewDrawable(
                    it,
                    TextViewDrawableMatcher.DrawablePosition.LEFT
                  )
                },
                statusLabelTintColorResId?.let {
                  withViewBackgroundTintResId(
                    getTargetContext(),
                    it
                  )
                }
              ),
            )
          )
        )
      )
  }
}
