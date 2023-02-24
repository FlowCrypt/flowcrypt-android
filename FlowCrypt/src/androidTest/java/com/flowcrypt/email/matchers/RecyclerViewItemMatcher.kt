/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * @author Denys Bondarenko
 */
class RecyclerViewItemMatcher(val matcher: Matcher<View>) :
  BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
  public override fun matchesSafely(recyclerView: RecyclerView): Boolean {
    val adapter = recyclerView.adapter ?: throw IllegalStateException("adapter is not present")
    for (position in 0 until adapter.itemCount) {
      val type = adapter.getItemViewType(position)
      val holder = adapter.createViewHolder(recyclerView, type)
      adapter.onBindViewHolder(holder, position)
      if (matcher.matches(holder.itemView)) {
        return true
      }
    }
    return false
  }

  override fun describeTo(description: Description) {}
}
