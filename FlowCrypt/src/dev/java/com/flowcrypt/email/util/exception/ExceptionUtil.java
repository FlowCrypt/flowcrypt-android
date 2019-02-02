/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

/**
 * This class describes methods for a work with {@link Exception}. The development version.
 *
 * @author Denis Bondarenko
 * Date: 02/01/2019
 * Time: 6:53 PM
 * E-mail: DenBond7@gmail.com
 */

public class ExceptionUtil extends ExceptionResolver {

  public static void handleError(Exception e) {
    if (ExceptionResolver.isHandlingNeeded(e)) {
      //Don't modify it
    }
  }

  public static void handleError(Boolean isError, String title, String stack_trace, String details) {
    //Don't modify it
  }
}
