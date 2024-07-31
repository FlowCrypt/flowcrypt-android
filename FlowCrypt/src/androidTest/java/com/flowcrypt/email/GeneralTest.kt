/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class GeneralTest {
  @Test
  fun testParsingInternetAddressesWithIllegalCharacters() {
    val emailWithInvalidCharacters =
      "Test “Test Test” test, sometest, hello, test <test.test@test.test>"

    assertEquals("test.test@test.test", emailWithInvalidCharacters.asInternetAddress()?.address)
  }
}