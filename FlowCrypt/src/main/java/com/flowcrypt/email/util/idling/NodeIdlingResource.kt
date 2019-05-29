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
 * Date: 2/19/19
 * Time: 9:20 AM
 * E-mail: DenBond7@gmail.com
 */
public class NodeIdlingResource implements IdlingResource {
  @Nullable
  private volatile ResourceCallback mCallback;

  // Idleness is controlled with this boolean.
  private AtomicBoolean mIsIdleNow = new AtomicBoolean(false);

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
    mCallback = callback;
  }

  /**
   * Sets the new idle state, if isIdleNow is true, it pings the {@link ResourceCallback}.
   *
   * @param isIdleNow false if there are pending operations, true if idle.
   */
  public void setIdleState(boolean isIdleNow) {
    mIsIdleNow.set(isIdleNow);
    if (isIdleNow && mCallback != null) {
      mCallback.onTransitionToIdle();
    }
  }
}
