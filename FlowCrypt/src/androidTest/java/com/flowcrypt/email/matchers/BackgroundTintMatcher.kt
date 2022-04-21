/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import org.hamcrest.Description

/**
 * @author Denis Bondarenko
 *         Date: 4/21/22
 *         Time: 1:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class BackgroundTintMatcher(private val expectedBackgroundTintList: ColorStateList) :
  BoundedDiagnosingMatcher<View?, TextView>(TextView::class.java) {
  private var context: Context? = null

  override fun matchesSafely(textView: TextView, mismatchDescription: Description): Boolean {
    context = textView.context
    val actualBackgroundTintList = textView.backgroundTintList
    mismatchDescription
      .appendText("textView.backgroundTintList was ")
      .appendText(actualBackgroundTintList.toString())
    return actualBackgroundTintList == expectedBackgroundTintList
  }

  override fun describeMoreTo(description: Description) {
    description.appendText("textView.backgroundTintList is color with $expectedBackgroundTintList")
  }
}
