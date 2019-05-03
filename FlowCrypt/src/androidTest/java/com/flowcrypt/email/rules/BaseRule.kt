/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule

/**
 * @author Denis Bondarenko
 *         Date: 5/3/19
 *         Time: 12:42 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseRule : TestRule {
  protected val context = InstrumentationRegistry.getInstrumentation().context
  protected val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
}