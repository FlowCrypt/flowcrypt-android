/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author Denys Bondarenko
 */
class FlowCryptApplicationTest {
  @Test
  fun testCreatePGPainlessInstanceEnablesKeyParameterValidation() {
    val pgpainless = FlowCryptApplication.createPGPainlessInstance()

    assertTrue(pgpainless.algorithmPolicy.enableKeyParameterValidation)
  }
}
