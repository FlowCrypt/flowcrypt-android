/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule can rerun a task a few times if it succeeded. By default we have 3 attempts
 *
 * @author Denis Bondarenko
 *         Date: 9/30/20
 *         Time: 4:00 PM
 *         E-mail: DenBond7@gmail.com
 */
class RepeatRule(private val retryCount: Int = 3) : BaseRule() {
  override fun execute() {}

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        for (i in 0 until retryCount) {
          try {
            base.evaluate()
          } catch (t: Throwable) {
            System.err.println(description.displayName.toString() + ": run " + (i + 1) + " failed")
            throw t
          }
        }
      }
    }
  }
}
