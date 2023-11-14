/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getLastModificationDate
import com.flowcrypt.email.extensions.org.pgpainless.key.info.generateKeyCapabilitiesDrawable
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPrimaryKey
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusIcon
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusText
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragmentArgs
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
import org.pgpainless.key.info.KeyRingInfo
import java.util.Date

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
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
  private val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())

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
    testPrimaryKeyDetails()

    //Thread.sleep(30000)
  }

  private fun testPrimaryKeyDetails() {
    onView(withId(R.id.textViewPrimaryKeyFingerprint))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            GeneralUtil.doSectionsInText(
              originalString = keyRingInfo.fingerprint.toString(),
              groupSize = 4
            )
          )
        )
      )

    val bitStrength =
      if (keyRingInfo.publicKey.bitStrength != -1) keyRingInfo.publicKey.bitStrength else null
    val algoWithBits = keyRingInfo.algorithm.name + (bitStrength?.let { "/$it" } ?: "")

    onView(withId(R.id.textViewPrimaryKeyAlgorithm))
      .check(matches(isDisplayed()))
      .check(matches(withText(algoWithBits)))

    onView(withId(R.id.textViewPrimaryKeyCreated))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_created,
              dateFormat.format(Date(keyRingInfo.creationDate.time))
            )
          )
        )
      )

    onView(withId(R.id.textViewPrimaryKeyModified))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_modified,
              dateFormat.format(
                requireNotNull(
                  keyRingInfo.getPrimaryKey()?.getLastModificationDate()
                )
              )
            )
          )
        )
      )

    onView(withId(R.id.textViewPrimaryKeyExpiration))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            keyRingInfo.primaryKeyExpirationDate?.time?.let {
              getResString(R.string.expires, dateFormat.format(Date(it)))
            } ?: getResString(
              R.string.expires, getResString(R.string.never)
            )
          )
        )
      )

    onView(withId(R.id.textViewPrimaryKeyCapabilities))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withTextViewDrawable(
            drawable = keyRingInfo.generateKeyCapabilitiesDrawable(
              getTargetContext(), keyRingInfo.getPrimaryKey()?.keyID ?: 0
            ),
            drawablePosition = TextViewDrawableMatcher.DrawablePosition.RIGHT
          )
        )
      )

    onView(
      allOf(
        withId(R.id.textViewStatusValue),
        hasSibling(withId(R.id.textViewMasterKey))
      )
    )
      .check(matches(isDisplayed()))
      .check(
        matches(
          allOf(
            withText(
              keyRingInfo.getStatusText(getTargetContext())
            ),
            /*withViewBackgroundTint(
              getTargetContext(),
              requireNotNull(
                keyRingInfo.getColorStateListDependsOnStatus(
                  getTargetContext()
                )
              )
            ),*/
            withTextViewDrawable(
              resourceId = keyRingInfo.getStatusIcon(),
              drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
            )
          )
        )
      )
  }

  companion object {
    const val EMAIL = "rich@flowcrypt.test"
    const val NAME = "Rich"
  }
}