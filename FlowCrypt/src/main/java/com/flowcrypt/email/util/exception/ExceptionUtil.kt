/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import org.acra.ACRA

/**
 * This class describes methods for a work with [Exception]
 *
 * @author Denys Bondarenko
 */

class ExceptionUtil {
  companion object {
    /**
     * Handle an input [Exception] by [ACRA].
     *
     * @param e An input [Exception]
     */
    fun handleError(e: Throwable?) {
      e ?: return
      if (ExceptionResolver.isHandlingNeeded(e)) {
        if (ACRA.isInitialised) {
          ACRA.errorReporter.handleException(ManualHandledException(e))
        }
      }
    }
  }
}
