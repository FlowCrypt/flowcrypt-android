/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher


/**
 * @author Denys Bondarenko
 */
class TextInputLayoutErrorMatcher(private val expectedHint: String) :
  TypeSafeDiagnosingMatcher<View>() {
  override fun describeTo(description: Description) {
    description.appendText("TextInputLayout with error = \"$expectedHint\"")
  }

  override fun matchesSafely(item: View?, mismatchDescription: Description?): Boolean {
    val actualHint = (item as? TextInputLayout)?.error?.toString()
    val matches: Boolean = expectedHint == actualHint
    if (!matches && mismatchDescription !is Description.NullDescription) {
      mismatchDescription
        ?.appendText("Actual was: $actualHint")
    }
    return matches
  }
}
