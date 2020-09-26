/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule can rerun a task a few times in it once failed. By default we have 3 attempts
 *
 * @author Denis Bondarenko
 *         Date: 9/26/20
 *         Time: 8:47 AM
 *         E-mail: DenBond7@gmail.com
 */
class RetryRule(private val retryCount: Int = 3) : BaseRule() {
  override fun apply(base: Statement, description: Description): Statement? {
    return statement(base, description)
  }

  private fun statement(base: Statement, description: Description): Statement? {
    return object : Statement() {
      override fun evaluate() {
        var caughtThrowable: Throwable? = null
        for (i in 0 until retryCount) {
          try {
            base.evaluate()
            return
          } catch (t: Throwable) {
            caughtThrowable = t
            System.err.println(description.displayName.toString() + ": run " + (i + 1) + " failed")
          }
        }
        System.err.println(description.displayName.toString() + ": giving up after " + retryCount + " failures")
        caughtThrowable?.let { throw it } ?: throw RuntimeException("Something went wrong")
      }
    }
  }
}