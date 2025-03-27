/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */
package com.flowcrypt.email.viewaction

import android.view.View
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import org.hamcrest.Matcher

/**
 * Based on [androidx.test.espresso.contrib.ViewPagerActions]
 *
 * @author Denys Bondarenko
 */
object ViewPager2Actions {
  private const val DEFAULT_SMOOTH_SCROLL = false

  fun scrollRight(smoothScroll: Boolean = DEFAULT_SMOOTH_SCROLL): ViewAction {
    return object : ViewPager2ScrollAction() {
      override fun getDescription() = "ViewPager2 move one page to the right"
      override fun performScroll(viewPager2: ViewPager2?) {
        val current = viewPager2?.currentItem ?: throw IllegalStateException()
        viewPager2.setCurrentItem(current + 1, smoothScroll)
      }
    }
  }

  fun scrollLeft(smoothScroll: Boolean = DEFAULT_SMOOTH_SCROLL): ViewAction {
    return object : ViewPager2ScrollAction() {
      override fun getDescription() = "ViewPager2 move one page to the left"
      override fun performScroll(viewPager2: ViewPager2?) {
        val current = viewPager2?.currentItem ?: throw IllegalStateException()
        viewPager2.setCurrentItem(current - 1, smoothScroll)
      }
    }
  }

  fun scrollToLast(smoothScroll: Boolean = DEFAULT_SMOOTH_SCROLL): ViewAction {
    return object : ViewPager2ScrollAction() {
      override fun getDescription() = "ViewPager2 move to last page"
      override fun performScroll(viewPager2: ViewPager2?) {
        val size = viewPager2?.adapter?.itemCount ?: throw IllegalStateException()
        if (size > 0) {
          viewPager2.setCurrentItem(size - 1, smoothScroll)
        }
      }
    }
  }

  fun scrollToFirst(smoothScroll: Boolean = DEFAULT_SMOOTH_SCROLL): ViewAction {
    return object : ViewPager2ScrollAction() {
      override fun getDescription() = "ViewPager2 move to first page"
      override fun performScroll(viewPager2: ViewPager2?) {
        val size = viewPager2?.adapter?.itemCount ?: throw IllegalStateException()
        if (size > 0) {
          viewPager2.setCurrentItem(0, smoothScroll)
        }
      }
    }
  }

  fun scrollToPage(page: Int, smoothScroll: Boolean = DEFAULT_SMOOTH_SCROLL): ViewAction {
    return object : ViewPager2ScrollAction() {
      override fun getDescription() = "ViewPager2 move to page"
      override fun performScroll(viewPager2: ViewPager2?) {
        viewPager2?.setCurrentItem(page, smoothScroll)
      }
    }
  }

  private class CustomViewPager2Listener : OnPageChangeCallback(), IdlingResource {
    private var currentState = ViewPager2.SCROLL_STATE_IDLE
    private var idlingResourceResourceCallback: IdlingResource.ResourceCallback? = null
    var needsIdle = false

    override fun registerIdleTransitionCallback(
      resourceCallback: IdlingResource.ResourceCallback?
    ) {
      idlingResourceResourceCallback = resourceCallback
    }

    override fun getName() = "View pager listener"

    override fun isIdleNow(): Boolean {
      return if (!needsIdle) {
        true
      } else {
        currentState == ViewPager2.SCROLL_STATE_IDLE
      }
    }

    override fun onPageSelected(position: Int) {
      if (currentState == ViewPager2.SCROLL_STATE_IDLE) {
        idlingResourceResourceCallback?.onTransitionToIdle()
      }
    }

    override fun onPageScrollStateChanged(state: Int) {
      currentState = state
      if (currentState == ViewPager2.SCROLL_STATE_IDLE) {
        idlingResourceResourceCallback?.onTransitionToIdle()
      }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
  }

  abstract class ViewPager2ScrollAction : ViewAction {
    override fun getConstraints(): Matcher<View?> = ViewMatchers.isDisplayed()

    override fun perform(uiController: UiController, view: View?) {
      (view as? ViewPager2)?.apply {
        val customListener = CustomViewPager2Listener()
        registerOnPageChangeCallback(customListener)
        try {
          IdlingRegistry.getInstance().register(customListener)
          uiController.loopMainThreadUntilIdle()
          performScroll(this)
          uiController.loopMainThreadUntilIdle()
          customListener.needsIdle = true
          uiController.loopMainThreadUntilIdle()
          customListener.needsIdle = false
        } finally {
          IdlingRegistry.getInstance().unregister(customListener)
          unregisterOnPageChangeCallback(customListener)
        }
      }
    }

    protected abstract fun performScroll(viewPager2: ViewPager2?)
  }
}

