/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
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
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import org.hamcrest.Matchers.allOf
import org.pgpainless.algorithm.PublicKeyAlgorithm
import org.pgpainless.key.OpenPgpFingerprint
import org.pgpainless.key.info.KeyRingInfo
import java.util.Date

/**
 * @author Denys Bondarenko
 */
abstract class BasePublicKeyDetailsTest : BaseTest(), AddAccountToDatabaseRuleInterface {
  private val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())
  protected fun testPrimaryKeyDetails(keyRingInfo: KeyRingInfo) {
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
            withViewBackgroundTint(
              requireNotNull(keyRingInfo.getColorStateListDependsOnStatus(getTargetContext()))
            ),
            withTextViewDrawable(
              resourceId = keyRingInfo.getStatusIcon(),
              drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
            )
          )
        )
      )
  }

  protected fun testUserIds(keyRingInfo: KeyRingInfo) {
    keyRingInfo.userIds.forEachIndexed { index, userId ->
      onView(withId(R.id.recyclerViewUserIds))
        .perform(
          scrollTo<RecyclerView.ViewHolder>(
            withText(getResString(R.string.template_user, index + 1, userId))
          )
        )
    }
  }

  protected fun testSubKeys(keyRingInfo: KeyRingInfo) {
    for (subKey in keyRingInfo.getPubKeysWithoutPrimary()) {
      val algorithm = PublicKeyAlgorithm.requireFromId(subKey.algorithm)
      val bitStrength = if (subKey.bitStrength != -1) subKey.bitStrength else null
      val algoWithBits = algorithm.name + (bitStrength?.let { "/$it" } ?: "")

      val expirationDate = subKey.getExpirationDate()
      val expirationText = if (expirationDate == null) {
        getResString(R.string.expires, getResString(R.string.never))
      } else {
        getResString(R.string.expires, dateFormat.format(expirationDate))
      }

      onView(withId(R.id.recyclerViewSubKeys))
        .check(
          matches(
            hasItem(
              allOf(
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyFingerprint),
                    withText(
                      GeneralUtil.doSectionsInText(
                        originalString = OpenPgpFingerprint.of(subKey).toString(), groupSize = 4
                      )
                    )
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyAlgorithm),
                    withText(algoWithBits)
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyCreated),
                    withText(
                      getResString(
                        R.string.template_created,
                        dateFormat.format(Date(subKey.creationTime.time))
                      )
                    )
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyModified),
                    withText(
                      getResString(
                        R.string.template_modified,
                        dateFormat.format(Date(subKey.getLastModificationDate().time))
                      )
                    )
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyExpiration),
                    withText(expirationText)
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewKeyCapabilities),
                    withTextViewDrawable(
                      drawable = keyRingInfo.generateKeyCapabilitiesDrawable(
                        getTargetContext(), subKey.keyID
                      ),
                      drawablePosition = TextViewDrawableMatcher.DrawablePosition.RIGHT
                    )
                  )
                ),
                hasDescendant(
                  allOf(
                    withId(R.id.textViewStatusValue),
                    withText(subKey.getStatusText(getTargetContext())),
                    withViewBackgroundTint(
                      requireNotNull(subKey.getStatusColorStateList(getTargetContext()))
                    ),
                    withTextViewDrawable(
                      resourceId = subKey.getStatusIconResId(),
                      drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
                    )
                  )
                )
              )
            )
          )
        )
    }
  }
}