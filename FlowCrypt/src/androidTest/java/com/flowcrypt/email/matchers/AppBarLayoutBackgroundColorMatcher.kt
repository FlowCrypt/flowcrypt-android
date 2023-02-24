/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.appbar.AppBarLayout
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class AppBarLayoutBackgroundColorMatcher(val color: Int) :
  BoundedMatcher<View, AppBarLayout>(AppBarLayout::class.java) {
  public override fun matchesSafely(appBarLayout: AppBarLayout): Boolean {
    return if (appBarLayout.background is ColorDrawable) {
      color == (appBarLayout.background as ColorDrawable).color
    } else false
  }

  override fun describeTo(description: Description) {
    description.appendText("Background color AppBarLayout: $color")
  }
}
