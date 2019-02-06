/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import com.flowcrypt.email.js.core.JavaScriptError;
import com.flowcrypt.email.js.core.JavaScriptReport;

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
  public static void handleError(Exception e) {
    if (ExceptionResolver.isHandlingNeeded(e)) {
      if (ACRA.isInitialised()) {
        ACRA.getErrorReporter().handleException(new ManualHandledException(e));
      }
    }
  }

  /**
   * Handle an error from Js.
   */
  public static void handleError(Boolean isError, String title, String stack_trace, String details) {
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_TITLE", title);
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_STACK_TRACE", stack_trace);
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_DETAILS", details);
    if (isError) {
      ACRA.getErrorReporter().handleSilentException(new JavaScriptError(title));
    } else {
      ACRA.getErrorReporter().handleSilentException(new JavaScriptReport(title));
    }
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_TITLE");
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_STACK_TRACE");
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_DETAILS");
  }
}
