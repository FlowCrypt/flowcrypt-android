/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import org.hamcrest.Matchers
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule can be used to throw(handle) all external intents. It must be call only after [IntentsTestRule]
 *
 * @author Denis Bondarenko
 *         Date: 7/17/19
 *         Time: 11:51 AM
 *         E-mail: DenBond7@gmail.com
 */
class StubAllExternalIntentsRule : BaseRule() {
  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        Intents.intending(Matchers.not<Intent>(IntentMatchers.isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        base.evaluate()
      }
    }
  }
}