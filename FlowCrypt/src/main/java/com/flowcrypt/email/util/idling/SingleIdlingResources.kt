/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.idling;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import androidx.test.espresso.IdlingResource;

/**
 * See details here https://github.com/googlesamples/android-testing/blob/master/ui/espresso/IdlingResourceSample/app
 * /src/main/java/com/example/android/testing/espresso/IdlingResourceSample/IdlingResource/SimpleIdlingResource.java
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 3:59 PM
 * E-mail: DenBond7@gmail.com
 */
public class SingleIdlingResources implements IdlingResource {
  @Nullable
  private volatile ResourceCallback resourceCallback;

  // Idleness is controlled with this boolean.
  private AtomicBoolean mIsIdleNow = new AtomicBoolean(true);

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public boolean isIdleNow() {
    return mIsIdleNow.get();
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback callback) {
    resourceCallback = callback;
  }

  /**
   * Sets the new idle state, if isIdleNow is true, it pings the {@link ResourceCallback}.
   *
   * @param isIdleNow false if there are pending operations, true if idle.
   */
  public void setIdleState(boolean isIdleNow) {
    if (mIsIdleNow.get() != isIdleNow) {
      mIsIdleNow.set(isIdleNow);
      if (isIdleNow && resourceCallback != null) {
        resourceCallback.onTransitionToIdle();
      }
    }
  }
}
