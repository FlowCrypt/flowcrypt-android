/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 9/16/20
 *         Time: 1:59 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@DebugTestAnnotation
@RunWith(AndroidJUnit4::class)
class DebugTestingTest {
  @Test
  @DoesNotNeedMailserver
  fun alwaysSuccessTest() {
  }
}