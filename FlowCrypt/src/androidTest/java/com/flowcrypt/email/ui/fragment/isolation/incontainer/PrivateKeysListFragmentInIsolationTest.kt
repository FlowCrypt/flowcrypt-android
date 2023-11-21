/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers
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
    val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyRingDetails = pgpKeyRingDetails,
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
      keyOwnerEmail = "default@flowcrypt.test",
      hasManyUserIds = false,
      fingerprint = "3DEBE9F677D5B9BB38E5A244225F8023C20D0957",
      creationDate = dateFormat.format(Date(pgpKeyRingDetails.created)),
      expirationDate = getResString(
        R.string.key_expiration,
        getResString(R.string.key_does_not_expire)
      ),
      statusLabelText = getResString(R.string.valid),
      statusLabelIconResId = R.drawable.ic_baseline_gpp_good_16,
      statusLabelTintColorResId = R.color.colorPrimary,
      usableForEncryption = true,
      usableForSigning = true
    )
  }

  @Test
  fun testShowRevokedKey() {
    val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_secondKey_prv_strong_revoked.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyRingDetails = pgpKeyRingDetails,
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
      keyOwnerEmail = "default@flowcrypt.test",
      hasManyUserIds = false,
      fingerprint = "45F0A5260A80F238598DD081C669001D0A6DCAC8",
      creationDate = dateFormat.format(Date(pgpKeyRingDetails.created)),
      expirationDate = getResString(
        R.string.key_expiration,
        getResString(R.string.key_does_not_expire)
      ),
      statusLabelText = getResString(R.string.revoked),
      statusLabelIconResId = R.drawable.ic_outline_warning_amber_16,
      statusLabelTintColorResId = R.color.red,
      usableForEncryption = false,
      usableForSigning = false
    )
  }

  @Test
  fun testShowExpiredKey() {
    val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/expired@flowcrypt.test_prv_default.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyRingDetails = pgpKeyRingDetails,
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

    assertNotNull(pgpKeyRingDetails.expiration)
    val expiration = requireNotNull(pgpKeyRingDetails.expiration)
    val expectedExpirationDate = "Jan 1, 2011"
    val actualExpirationDate = dateFormat.format(Date(expiration))
    assertEquals(expectedExpirationDate, actualExpirationDate)

    doBaseCheckingForKey(
      keyOwner = "Expired Key",
      keyOwnerEmail = "expired@flowcrypt.test",
      hasManyUserIds = false,
      fingerprint = "599132F15A04487AA6356C7F717B789F05D874DA",
      creationDate = dateFormat.format(Date(pgpKeyRingDetails.created)),
      expirationDate = getResString(R.string.key_expiration, actualExpirationDate),
      statusLabelText = getResString(R.string.expired),
      statusLabelIconResId = R.drawable.ic_outline_warning_amber_16,
      statusLabelTintColorResId = R.color.orange,
      usableForEncryption = false,
      usableForSigning = false
    )
  }

  @Test
  fun testShowKeyWithManyUserIds() {
    val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/rich@flowcrypt.test_prv_default.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyRingDetails = pgpKeyRingDetails,
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

    doBaseCheckingForKey(
      keyOwner = "Rich",
      keyOwnerEmail = "rich@flowcrypt.test",
      hasManyUserIds = true,
      fingerprint = "670F49046294213EA166DBA6AAD5550DFBC79F22",
      creationDate = dateFormat.format(Date(pgpKeyRingDetails.created)),
      expirationDate = getResString(
        R.string.key_expiration,
        getResString(R.string.key_does_not_expire)
      ),
      statusLabelText = getResString(R.string.valid),
      statusLabelIconResId = R.drawable.ic_baseline_gpp_good_16,
      statusLabelTintColorResId = R.color.colorAccent,
      usableForEncryption = false,
      usableForSigning = true
    )
  }

  private fun doBaseCheckingForKey(
    keyOwner: String,
    keyOwnerEmail: String,
    hasManyUserIds: Boolean,
    fingerprint: String,
    creationDate: String,
    expirationDate: String,
    statusLabelText: String,
    statusLabelIconResId: Int,
    statusLabelTintColorResId: Int,
    usableForEncryption: Boolean,
    usableForSigning: Boolean,
  ) {
    onView(withId(R.id.recyclerViewKeys))
      .check(
        matches(
          CustomMatchers.hasItem(
            withChild(
              allOf(
                hasSibling(
                  allOf(
                    withId(R.id.textViewPrimaryUserOrEmail),
                    withText(keyOwner)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewPrimaryUserEmail),
                    if (keyOwnerEmail == keyOwner) {
                      withEffectiveVisibility(Visibility.GONE)
                    } else {
                      withText(keyOwnerEmail)
                    }
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewManyUserIds),
                    withEffectiveVisibility(if (hasManyUserIds) Visibility.VISIBLE else Visibility.GONE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewCreationDate),
                    withText(creationDate)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewExpiration),
                    withText(expirationDate)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewFingerprint),
                    withText(
                      GeneralUtil.doSectionsInText(
                        originalString = fingerprint,
                        groupSize = 4
                      )
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.textViewStatus),
                    withText(statusLabelText),
                    withViewBackgroundTintResId(
                      getTargetContext(),
                      statusLabelTintColorResId
                    ),
                    withTextViewDrawable(
                      resourceId = statusLabelIconResId,
                      drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
                    )
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewEncryptionFlag),
                    withEffectiveVisibility(if (usableForEncryption) Visibility.VISIBLE else Visibility.GONE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewSignFlag),
                    withEffectiveVisibility(if (usableForSigning) Visibility.VISIBLE else Visibility.GONE)
                  )
                ),
                hasSibling(
                  allOf(
                    withId(R.id.imageViewAuthFlag),
                    withEffectiveVisibility(Visibility.GONE)
                  )
                )
              )
            )
          )
        )
      )
  }
}
