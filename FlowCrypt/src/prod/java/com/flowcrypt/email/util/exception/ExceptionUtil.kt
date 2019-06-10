/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import org.acra.ACRA

/**
 * This class describes methods for a work with [Exception]
 *
 * @author Denis Bondarenko
 * Date: 25.01.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */

class ExceptionUtil {
  companion object {
    /**
     * Handle an input [Exception] by [ACRA].
     *
     * @param e An input [Exception]
     */
    @JvmStatic
    fun handleError(e: Throwable) {
      if (ExceptionResolver.isHandlingNeeded(e)) {
        if (ACRA.isInitialised()) {
          ACRA.getErrorReporter().handleException(ManualHandledException(e))
        }
      }
    }
  }
}
