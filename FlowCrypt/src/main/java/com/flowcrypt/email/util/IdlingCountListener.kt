/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import java.util.concurrent.atomic.AtomicInteger

/**
 * This listener will be used to help debug idling issues for Instrumentation UI tests.
 *
 * @author Denis Bondarenko
 *         Date: 12/9/22
 *         Time: 11:00 AM
 *         E-mail: DenBond7@gmail.com
 */
interface IdlingCountListener {
  fun incrementIdlingCount()
  fun decrementIdlingCount()

  companion object {
    fun handleIncrement(atomicInteger: AtomicInteger, clazz: Class<*>) {
      atomicInteger.incrementAndGet()
      LogsUtil.d(
        clazz.simpleName,
        clazz.simpleName + ":>>>> = " + atomicInteger + "|" + atomicInteger.hashCode()
      )
    }

    fun handleDecrement(atomicInteger: AtomicInteger, clazz: Class<*>) {
      atomicInteger.incrementAndGet()
      LogsUtil.d(
        clazz.simpleName,
        clazz.simpleName + ":<<<< = " + atomicInteger + "|" + atomicInteger.hashCode()
      )
    }

    fun printIdlingStats(atomicInteger: AtomicInteger, clazz: Class<*>) {
      LogsUtil.d(
        clazz.simpleName,
        clazz.simpleName + ":idlingCount = " + atomicInteger + "|" + atomicInteger.hashCode()
      )
    }
  }
}
