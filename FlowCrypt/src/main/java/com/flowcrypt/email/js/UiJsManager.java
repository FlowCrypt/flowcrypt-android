/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.app.Application;
import android.content.Context;
import android.os.Looper;

import com.flowcrypt.email.security.SecurityStorageConnector;

/**
 *
 * @author Denis Bondarenko
 * Date: 13.12.2017
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */

public final class UiJsManager {
  private static UiJsManager ourInstance;

  private SecurityStorageConnector securityStorageConnector;

  private UiJsManager(Context context) {
    this.securityStorageConnector = new SecurityStorageConnector(context);
  }

  /**
   * Call this method to init {@link UiJsManager}. Call this method on {@link Application#onCreate()}
   *
   * @param context Interface to global information about an application environment.
   */
  public static void init(Context context) {
    getInstance(context.getApplicationContext());
  }

  public static UiJsManager getInstance(Context context) {
    checkUIThread();

    if (ourInstance == null) {
      ourInstance = new UiJsManager(context);
    }
    return ourInstance;
  }

  public SecurityStorageConnector getStorageConnector() {
    return securityStorageConnector;
  }

  /**
   * The check that this method called from the UI thread.
   */
  private static void checkUIThread() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      throw new IllegalStateException("Can't use this class in the non-Ui thread");
    }
  }
}
