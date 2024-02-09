/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.fragment.PrivateToPublicKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PrivateToPublicKeyDetailsFragmentArgs
import com.flowcrypt.email.ui.base.AddAccountToDatabaseRuleInterface
import com.flowcrypt.email.ui.base.BasePublicKeyDetailsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class PrivateToPublicKeyDetailsFragmentInIsolationTest : BasePublicKeyDetailsTest(),
  AddAccountToDatabaseRuleInterface {

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val keyRingInfo = KeyRingInfo(
    requireNotNull(
      PgpKey.parseKeys(source = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey.toByteArray())
        .getAllKeys().firstOrNull()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<PrivateToPublicKeyDetailsFragment>(
      PrivateToPublicKeyDetailsFragmentArgs(
        fingerprint = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.fingerprint
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
}