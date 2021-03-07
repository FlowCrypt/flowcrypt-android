/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security

import org.junit.Test
import org.junit.Assert.assertEquals
import java.math.BigInteger

class PgpPwdTest {
    @Test
    fun testEstimateStrength() {
        val result = PgpPwd.estimateStrength(
                BigInteger("88946283684264"), PgpPwd.PwdType.PASSPHRASE)
        assertEquals(
                PgpPwd.PwdStrengthResult(
                    word = PgpPwd.Word(
                            match = "week",
                            word =  "poor",
                            bar = 30,
                            color = "darkred",
                            pass = false
                    ),
                    seconds = BigInteger.valueOf(1111829),
                    time = "2 weeks"
                ),
                result
        )
    }
}
