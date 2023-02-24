/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denys Bondarenko
 */
abstract class BaseRule : TestRule {
  protected val context: Context = InstrumentationRegistry.getInstrumentation().context
  protected val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

  abstract fun execute()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        execute()
        base.evaluate()
      }
    }
  }
}
