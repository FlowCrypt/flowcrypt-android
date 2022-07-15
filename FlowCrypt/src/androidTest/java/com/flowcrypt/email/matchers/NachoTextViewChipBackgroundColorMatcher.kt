/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.hootsuite.nachos.NachoTextView
import org.hamcrest.Description

/**
 * @author Denis Bondarenko
 *         Date: 4/22/21
 *         Time: 10:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class NachoTextViewChipBackgroundColorMatcher(
  private val chipText: String,
  private val backgroundColor: Int
) : BoundedMatcher<View, NachoTextView>(NachoTextView::class.java) {
  public override fun matchesSafely(nachoTextView: NachoTextView): Boolean {
    val expectedChip = nachoTextView.allChips.firstOrNull { it.text == chipText } ?: return false
    val pgpContactChipSpan = expectedChip as? PGPContactChipSpan ?: return false
    return backgroundColor == pgpContactChipSpan.chipBackgroundColor?.defaultColor
  }

  override fun describeTo(description: Description) {
    description.appendText("Chip details: text = $chipText, color = $backgroundColor")
  }
}
