/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.gms.common.internal.Preconditions.checkNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * @author Denys Bondarenko
 */
class ToolBarTitleMatcher(private val textMatcher: Matcher<String>) :
  BoundedMatcher<View, Toolbar>(Toolbar::class.java) {

  override fun matchesSafely(toolbar: Toolbar): Boolean {
    return textMatcher.matches(toolbar.title)
  }

  override fun describeTo(description: Description) {
    description.appendText("with toolbar title: ")
    textMatcher.describeTo(description)
  }

  companion object {
    @JvmStatic
    fun withText(textMatcher: String): Matcher<View> {
      return ToolBarTitleMatcher(checkNotNull(`is`(textMatcher)))
    }
  }
}
