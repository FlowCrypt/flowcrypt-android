/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.rules.RetryRule.Companion.MAX_RETRY_VALUE
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule can rerun a task a few times if it once failed.
 * It uses [retryCount] value from the constructor or can be overridden with [RetryOnFailure] annotation.
 * Max [retryCount] == [MAX_RETRY_VALUE]
 *
 * @author Denis Bondarenko
 *         Date: 9/26/20
 *         Time: 8:47 AM
 *         E-mail: DenBond7@gmail.com
 */
class RetryRule(private val retryCount: Int = 0) : BaseRule() {
  override fun execute() {}

  override fun apply(base: Statement, description: Description): Statement {
    return statement(base, description)
  }

  private fun statement(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val fromAnnotation = description
          .annotations
          .filterIsInstance<RetryOnFailure>()
          .firstOrNull()
          ?.value

        val attempts = if (fromAnnotation == null || fromAnnotation !in 1..MAX_RETRY_VALUE) {
          if (retryCount in 1..MAX_RETRY_VALUE) retryCount else 1
        } else {
          fromAnnotation
        }

        var caughtThrowable: Throwable? = null
        repeat(attempts) { times ->
          runCatching { base.evaluate() }
            .onSuccess { return }
            .onFailure {
              TestGeneralUtil.clearApp(targetContext)
              Thread.sleep(1000)
              caughtThrowable = it
              System.err.println(description.displayName.toString() + ": run $times failed")
            }
        }
        System.err.println(description.displayName.toString() + ": giving up after " + attempts + " failures")
        caughtThrowable?.let { throw it } ?: throw RuntimeException("Something went wrong")
      }
    }
  }

  companion object {
    private const val DEFAULT_RETRY_VALUE = 1
    private const val MAX_RETRY_VALUE = 100
    val DEFAULT = RetryRule(DEFAULT_RETRY_VALUE)
  }
}
