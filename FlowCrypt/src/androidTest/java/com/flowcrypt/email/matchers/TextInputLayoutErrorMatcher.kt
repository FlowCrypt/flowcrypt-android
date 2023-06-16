/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher


/**
 * @author Denys Bondarenko
 */
class TextInputLayoutErrorMatcher(private val expectedHint: String) : TypeSafeMatcher<View>() {
  override fun describeTo(description: Description) {
    description.appendText("TextInputLayout with error = \"$expectedHint\"")
  }

  override fun matchesSafely(view: View): Boolean {
    return expectedHint == (view as? TextInputLayout)?.error?.toString()
  }
}
