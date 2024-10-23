/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.androidx.viewpager2.widget

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly

/**
 * @author Denys Bondarenko
 */

/**
 * It's a temporary solution for ViewPager2 to improve nested scrolling.
 * Should be removed when https://issuetracker.google.com/issues/123006042 will be fixed
 *
 * Origin:
 * https://medium.com/nerd-for-tech/android-webview-part-2-webview-on-top-viewpager2-639181e6d6f5
 */
fun ViewPager2.reduceDragSensitivity(factor: Int = 4) {
  try {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * factor)
  } catch (e: Exception) {
    e.printStackTraceIfDebugOnly()
  }
}
