/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.flowcrypt.email.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import java.util.ArrayList

/**
 * A [ScrollView] that will never lock scrolling in a particular direction.
 *
 * Usually ScrollView will capture all touch events once a drag has begun. In some cases,
 * we want to delegate those touches to children as normal, even in the middle of a drag. This is
 * useful when there are childviews like a WebView that handles scrolling in the horizontal direction
 * even while the ScrollView drags vertically.
 *
 * This is only tested to work for ScrollViews where the content scrolls in one direction.
 *
 * See https://github.com/k9mail/k-9
 */
class NonLockingScrollView : NestedScrollView {
  /**
   * The list of children who should always receive touch events, and not have them intercepted.
   */
  private val childrenNeedingAllTouches = ArrayList<View>()
  private val hitFrame = Rect()

  /**
   * Whether or not the contents of this view is being dragged by one of the children in
   * [.childrenNeedingAllTouches].
   */
  private var inCustomDrag: Boolean = false
  private var skipWebViewScroll = true

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    val action = getActionMasked(ev)
    val isUp = action == MotionEvent.ACTION_UP

    if (isUp && inCustomDrag) {
      // An up event after a drag should be intercepted so that child views don't handle
      // click events falsely after a drag.
      inCustomDrag = false
      onTouchEvent(ev)
      return true
    }

    if (!inCustomDrag && !isEventOverChild(ev, childrenNeedingAllTouches)) {
      return super.onInterceptTouchEvent(ev)
    }

    // Note the normal scrollview implementation is to intercept all touch events after it has
    // detected a drag starting. We will handle this ourselves.
    inCustomDrag = super.onInterceptTouchEvent(ev)
    if (inCustomDrag) {
      onTouchEvent(ev)
    }

    // Don't intercept events - pass them on to children as normal.
    return false
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    setupDelegationOfTouchAndHierarchyChangeEvents()
  }

  override fun requestChildFocus(child: View, focused: View) {
    /*
     * Normally a ScrollView will scroll the child into view.
     * Prevent this when a MessageWebView is first touched,
     * assuming it already is at least partially in view.
     *
     */
    if (skipWebViewScroll && focused is EmailWebView && focused.getGlobalVisibleRect(Rect())) {
      skipWebViewScroll = false
      super.requestChildFocus(child, child)
      val parent = parent
      parent?.requestChildFocus(this, focused)
    } else {
      super.requestChildFocus(child, focused)
    }
  }

  private fun canViewReceivePointerEvents(child: View): Boolean {
    return child.visibility == View.VISIBLE || child.animation != null
  }

  private fun getActionMasked(ev: MotionEvent): Int {
    // Equivalent to MotionEvent.getActionMasked() which is in API 8+
    return ev.action and MotionEvent.ACTION_MASK
  }

  private fun setupDelegationOfTouchAndHierarchyChangeEvents() {
    val listener = HierarchyTreeChangeListener()
    setOnHierarchyChangeListener(listener)
    var i = 0
    val childCount = childCount
    while (i < childCount) {
      listener.onChildViewAdded(this, getChildAt(i))
      i++
    }
  }

  private fun isEventOverChild(ev: MotionEvent, children: List<View>): Boolean {
    val actionIndex = ev.actionIndex
    val x = ev.getX(actionIndex) + scrollX
    val y = ev.getY(actionIndex) + scrollY

    for (child in children) {
      if (!canViewReceivePointerEvents(child)) {
        continue
      }
      child.getHitRect(hitFrame)

      // child can receive the motion event.
      if (hitFrame.contains(x.toInt(), y.toInt())) {
        return true
      }
    }
    return false
  }

  private inner class HierarchyTreeChangeListener : OnHierarchyChangeListener {
    override fun onChildViewAdded(parent: View, child: View) {
      if (child is WebView) {
        childrenNeedingAllTouches.add(child)
      } else if (child is ViewGroup) {
        child.setOnHierarchyChangeListener(this)
        var i = 0
        val childCount = child.childCount
        while (i < childCount) {
          onChildViewAdded(child, child.getChildAt(i))
          i++
        }
      }
    }

    override fun onChildViewRemoved(parent: View, child: View) {
      if (child is WebView) {
        childrenNeedingAllTouches.remove(child)
      } else if (child is ViewGroup) {
        var i = 0
        val childCount = child.childCount
        while (i < childCount) {
          onChildViewRemoved(child, child.getChildAt(i))
          i++
        }
        child.setOnHierarchyChangeListener(null)
      }
    }
  }
}
