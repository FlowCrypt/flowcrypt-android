/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.idling

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * See details here https://github.com/googlesamples/android-testing/blob/master/ui/espresso/IdlingResourceSample/app
 * /src/main/java/com/example/android/testing/espresso/IdlingResourceSample/IdlingResource/SimpleIdlingResource.java
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 3:59 PM
 * E-mail: DenBond7@gmail.com
 */
class SingleIdlingResources : IdlingResource {
  @Volatile
  private var resourceCallback: IdlingResource.ResourceCallback? = null

  // Idleness is controlled with this boolean.
  private val mIsIdleNow = AtomicBoolean(true)

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
   */
  fun setIdleState(isIdleNow: Boolean) {
    if (mIsIdleNow.get() != isIdleNow) {
      mIsIdleNow.set(isIdleNow)
      if (isIdleNow && resourceCallback != null) {
        resourceCallback!!.onTransitionToIdle()
      }
    }
  }
}
