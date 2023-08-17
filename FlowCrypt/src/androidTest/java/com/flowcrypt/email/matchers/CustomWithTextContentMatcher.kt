/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import com.google.common.base.Preconditions
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.w3c.dom.Element

/**
 * @author Denys Bondarenko
 */
class CustomWithTextContentMatcher constructor(textContentMatcher: Matcher<String>) :
  TypeSafeMatcher<Element>() {
  private val textContentMatcher: Matcher<String>

  init {
    this.textContentMatcher =
      Preconditions.checkNotNull(textContentMatcher, "textContentMatcher cannot be null")
  }

  override fun matchesSafely(element: Element): Boolean {
    return textContentMatcher.matches(element.textContent.trim())
  }

  override fun describeTo(description: Description) {
    description.appendText("with text content: ")
    textContentMatcher.describeTo(description)
  }
}