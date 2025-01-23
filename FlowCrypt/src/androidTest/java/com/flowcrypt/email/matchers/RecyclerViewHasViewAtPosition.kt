/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Matcher

/**
 * @author Denys Bondarenko
 */
class RecyclerViewHasViewAtPosition(private val position: Int, private val matcher: Matcher<View>) :
  BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {

  override fun matchesSafely(item: RecyclerView?): Boolean {
    val viewHolder = item?.findViewHolderForAdapterPosition(position)
    return matcher.matches(viewHolder?.itemView)
  }

  override fun describeTo(description: org.hamcrest.Description?) {
    description?.appendText("has item at position $position : ")
    matcher.describeTo(description)
  }
}