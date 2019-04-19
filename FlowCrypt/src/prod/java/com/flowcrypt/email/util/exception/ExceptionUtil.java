/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import org.acra.ACRA;

/**
 * This class describes methods for a work with {@link Exception}
 *
 * @author Denis Bondarenko
 * Date: 25.01.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */

public class ExceptionUtil extends ExceptionResolver {
  /**
   * Handle an input {@link Exception} by {@link ACRA}.
   *
   * @param e An input {@link Exception}
   */
  public static void handleError(Throwable e) {
    if (ExceptionResolver.isHandlingNeeded(e)) {
      if (ACRA.isInitialised()) {
        ACRA.getErrorReporter().handleException(new ManualHandledException(e));
      }
    }
  }
}
