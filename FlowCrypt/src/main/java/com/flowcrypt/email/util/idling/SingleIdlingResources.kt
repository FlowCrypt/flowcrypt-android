/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.idling

import androidx.test.espresso.IdlingResource
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * See details here https://github.com/googlesamples/android-testing/blob/master/ui/espresso/IdlingResourceSample/app
 * /src/main/java/com/example/android/testing/espresso/IdlingResourceSample/IdlingResource/SimpleIdlingResource.java
 *
 * @author Denys Bondarenko
 */
class SingleIdlingResources(initialValue: Boolean = true, val delay: Long = 0) : IdlingResource {
  @Volatile
  private var resourceCallback: IdlingResource.ResourceCallback? = null

  // Idleness is controlled with this boolean.
  private val mIsIdleNow = AtomicBoolean(initialValue)

  private var timer: Timer? = null

  override fun getName(): String {
    return this.javaClass.name
  }

  override fun isIdleNow(): Boolean {
    return mIsIdleNow.get()
  }

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
    resourceCallback = callback
  }

  /**
   * Sets the new idle state, if isIdleNow is true, it pings the [ResourceCallback].
   *
   * @param isIdleNow false if there are pending operations, true if idle.
   * @param useDelay  false if we want to use delay for the current operation
   */
  fun setIdleState(isIdleNow: Boolean, useDelay: Boolean = false) {
    if (mIsIdleNow.get() != isIdleNow) {
      timer?.cancel()
      if (useDelay) {
        timer = Timer()
        timer?.schedule(
          object : TimerTask() {
            override fun run() {
              mIsIdleNow.set(isIdleNow)
              if (isIdleNow && resourceCallback != null) {
                resourceCallback?.onTransitionToIdle()
              }
            }
          }, delay
        )
      } else {
        mIsIdleNow.set(isIdleNow)
        if (isIdleNow && resourceCallback != null) {
          resourceCallback?.onTransitionToIdle()
        }
      }
    }
  }
}
