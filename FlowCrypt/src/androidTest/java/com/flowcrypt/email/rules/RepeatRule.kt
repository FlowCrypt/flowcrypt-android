/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.rules

import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule can rerun a task a few times if it succeeded. By default we have 3 attempts.
 * If [Description] contains [Repeat] annotation the last will be used.
 *
 * @author Denys Bondarenko
 */
class RepeatRule(private val retryCount: Int = 3) : BaseRule() {
  override fun execute() {}

  override fun apply(base: Statement, description: Description): Statement {
    val repeat = description.getAnnotation(Repeat::class.java)
    val attempts = repeat?.value ?: retryCount
    return object : Statement() {
      override fun evaluate() {
        for (i in 0 until attempts) {
          try {
            base.evaluate()
            println(description.displayName.toString() + ": run " + (i + 1) + " completed")
          } catch (t: Throwable) {
            System.err.println(description.displayName.toString() + ": run " + (i + 1) + " of $attempts failed")
            throw t
          }
        }
      }
    }
  }
}
