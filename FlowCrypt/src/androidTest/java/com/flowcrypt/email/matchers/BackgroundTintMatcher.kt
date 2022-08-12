/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import org.hamcrest.Description

/**
 * @author Denis Bondarenko
 *         Date: 4/21/22
 *         Time: 1:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class BackgroundTintMatcher(private val expectedBackgroundTintList: ColorStateList) :
  BoundedDiagnosingMatcher<View?, View>(View::class.java) {
  private var context: Context? = null

  override fun matchesSafely(view: View, mismatchDescription: Description): Boolean {
    context = view.context
    val actualBackgroundTintList = view.backgroundTintList
    return actualBackgroundTintList == expectedBackgroundTintList
  }

  override fun describeMoreTo(description: Description) {
    description.appendText("view.backgroundTintList is color with $expectedBackgroundTintList")
  }
}
