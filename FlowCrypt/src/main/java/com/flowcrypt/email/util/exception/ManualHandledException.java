/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.acra.ACRA;

/**
 * This class is using with {@link ACRA} when we handle some exception <b>manually</b> and send logs to the server.
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
