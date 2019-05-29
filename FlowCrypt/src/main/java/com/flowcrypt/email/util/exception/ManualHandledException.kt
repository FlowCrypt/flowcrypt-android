/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * This class is using with ACRA when we handle some exception <b>manually</b> and send logs to the server.
 *
 * @author Denis Bondarenko
 * Date: 23.01.2018
 * Time: 12:17
 * E-mail: DenBond7@gmail.com
 */

public class ManualHandledException extends FlowCryptException {
  public ManualHandledException(String message) {
    super(message);
  }

  public ManualHandledException(String message, Throwable cause) {
    super(message, cause);
  }

  public ManualHandledException(Throwable cause) {
    super("Handled manually:", cause);
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public ManualHandledException(String message, Throwable cause, boolean enableSuppression, boolean
      writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
