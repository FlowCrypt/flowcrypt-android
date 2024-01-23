/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denys Bondarenko
 */

class RepeatedRule : BaseRule() {
  override fun execute() {}

  override fun apply(base: Statement, description: Description): Statement {
    val repeat = description.getAnnotation(Repeat::class.java) ?: return base
    val times = repeat.value
    return RepeatStatement(base, times)
  }

  private class RepeatStatement(
    private val statement: Statement,
    private val repeat: Int
  ) : Statement() {

    override fun evaluate() {
      for (i in 0 until repeat) {
        statement.evaluate()
      }
    }
  }
}
