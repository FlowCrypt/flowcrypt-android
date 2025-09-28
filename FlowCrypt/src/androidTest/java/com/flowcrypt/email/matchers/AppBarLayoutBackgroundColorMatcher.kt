/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.shape.MaterialShapeDrawable
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class AppBarLayoutBackgroundColorMatcher(val color: Int) :
  BoundedMatcher<View, AppBarLayout>(AppBarLayout::class.java) {
  public override fun matchesSafely(appBarLayout: AppBarLayout): Boolean {
    return if (appBarLayout.background is MaterialShapeDrawable) {
      (appBarLayout.background as MaterialShapeDrawable).fillColor?.defaultColor == color
    } else {
      false
    }
  }

  override fun describeTo(description: Description) {
    description.appendText("Background color AppBarLayout: $color")
  }
}
