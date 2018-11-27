/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.app.Application;
import android.content.Context;
import android.os.Looper;

import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.io.IOException;

/**
 * This class is the right way for using {@link Js} in the UI thread.
 *
 * @author Denis Bondarenko
 * Date: 13.12.2017
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */

public final class JsForUiManager {
  private static JsForUiManager ourInstance;

  private Js js;

  private JsForUiManager(Context context) {
    try {
      this.js = new Js(context, new SecurityStorageConnector(context));
    } catch (IOException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Call this method to init {@link JsForUiManager}. Call this method on {@link Application#onCreate()}
   *
   * @param context Interface to global information about an application environment.
   */
  public static void init(Context context) {
    getInstance(context.getApplicationContext());
  }

  public static JsForUiManager getInstance(Context context) {
    checkIsUIThread();

    if (ourInstance == null) {
      ourInstance = new JsForUiManager(context);
    }
    return ourInstance;
  }

  public Js getJs() {
    return js;
  }

  /**
   * The check that this method called from the UI thread.
   */
  private static void checkIsUIThread() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      throw new IllegalStateException("Can't use this class in the non-Ui thread");
    }
  }
}
