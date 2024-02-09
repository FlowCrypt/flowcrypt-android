/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getExpirationDate
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getLastModificationDate
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusColorStateList
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusIconResId
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusText
import com.flowcrypt.email.extensions.org.pgpainless.key.info.generateKeyCapabilitiesDrawable
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getColorStateListDependsOnStatus
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPrimaryKey
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPubKeysWithoutPrimary
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusIcon
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusText
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragmentArgs
import com.flowcrypt.email.ui.base.BasePublicKeyDetailsTest
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.algorithm.PublicKeyAlgorithm
import org.pgpainless.key.OpenPgpFingerprint
import org.pgpainless.key.info.KeyRingInfo
import java.util.Date

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class PublicKeyDetailsInIsolationTest : BasePublicKeyDetailsTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/rich@flowcrypt.test_pub.asc")
  private val keyRingInfo = KeyRingInfo(
    requireNotNull(
      PgpKey.parseKeys(source = keyDetails.publicKey.toByteArray()).getAllKeys().firstOrNull()
    )
  )
  private val recipientEntity = RecipientEntity(
    email = EMAIL,
    name = NAME
  )
  private val publicKeyEntity = keyDetails.toPublicKeyEntity(EMAIL).copy(id = 12)

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(
      AddRecipientsToDatabaseRule(
        listOf(RecipientWithPubKeys(recipientEntity, listOf(publicKeyEntity)))
      )
    )
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<PublicKeyDetailsFragment>(
      fragmentArgs = PublicKeyDetailsFragmentArgs(
        recipientEntity = recipientEntity,
        publicKeyEntity = publicKeyEntity
      ).toBundle()
    )
  }

  @Test
  fun testPublicKeyDetails() {
    Thread.sleep(1000)

    testPrimaryKeyDetails(keyRingInfo)
    testUserIds(keyRingInfo)
    testSubKeys(keyRingInfo)
  }

  companion object {
    const val EMAIL = "rich@flowcrypt.test"
    const val NAME = "Rich"
  }
}