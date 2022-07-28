/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.chip.Chip
import org.hamcrest.Description

class ChipCloseIconAvailabilityMatcher(
  private val isCloseIconVisible: Boolean
) : BoundedMatcher<View, Chip>(Chip::class.java) {
  public override fun matchesSafely(chip: Chip): Boolean {
    return chip.isCloseIconVisible == isCloseIconVisible
  }

  override fun describeTo(description: Description) {
    description.appendText("Chip details: isCloseIconVisible = $isCloseIconVisible")
  }
}
