/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.test.espresso.Root
import org.hamcrest.Matcher

/**
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 5:20 PM
 * E-mail: DenBond7@gmail.com
 */

class CustomMatchers {
  companion object {
    @JvmStatic
    fun withDrawable(resourceId: Int): Matcher<View> {
      return DrawableMatcher(resourceId)
    }

    @JvmStatic
    fun emptyDrawable(): Matcher<View> {
      return DrawableMatcher(DrawableMatcher.EMPTY)
    }

    @JvmStatic
    fun isToastDisplayed(): Matcher<Root> {
      return ToastMatcher()
    }

    @JvmStatic
    fun withToolBarText(textMatcher: String): Matcher<View> {
      return ToolBarTitleMatcher.withText(textMatcher)
    }
  }
}
