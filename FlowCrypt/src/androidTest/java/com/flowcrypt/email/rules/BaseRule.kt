/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule

/**
 * @author Denis Bondarenko
 *         Date: 5/3/19
 *         Time: 12:42 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseRule : TestRule {
  protected val context: Context = InstrumentationRegistry.getInstrumentation().context
  protected val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
}
