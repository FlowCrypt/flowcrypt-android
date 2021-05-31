/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.security.model.Algo
import com.flowcrypt.email.security.model.KeyId
import com.flowcrypt.email.security.model.PgpKeyDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class PgpKeyTest {

  @Test
  fun testParseKeysWithNormalKey() {
    val pubKey = TestKeys.KEYS["rsa1"]!!.publicKey
    val result = PgpKey.parseKeys(pubKey.toByteArray())

    val expected = PgpKeyDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false, privateKey = null,
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "Version: FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "mQENBFwBWOEBB/9uIqBYIPDQbBqHMvGXhgnm+b2i5rNLXrrGoalrp7wYQ654Zln/\n" +
          "+ffxzttRLRiwRQAOG0z78aMDXAHRfI9d3GaRKTkhTqVY+C02E8NxgB3+mbSsF0Ui\n" +
          "+oh1//LT1ic6ZnISCA7Q2h2U/DSAPNxDZUMu9kjh9TjkKlR81fiAlxuD05ivRxCn\n" +
          "mZnzqZtHoUvvCqsENgRjO9a5oWpMwtdItjdRFF7UFKYpfeA+ct0uUNMRVdPK7MXB\n" +
          "Er2FdWiKN1K21dQ1pWiAwj/5cTA8hu5Jue2RcF8FcPfsniRihQkNqtLDsfY5no1B\n" +
          "3xeSnyO2SES1bAHw8ObXZn/C/6jxFztkn4NbABEBAAG0EFRlc3QgPHRAZXN0LmNv\n" +
          "bT6JATUEEAEIACkFAlwBWOEGCwkHCAMCCRA6MPTMCpqPEAQVCAoCAxYCAQIZAQIb\n" +
          "AwIeAQAA1pMH/R9oEVHaTdEzs/jbsfJk6xm2oQ/G7KewtSqawAC6nou0+GKvgICx\n" +
          "vkNK+BivMLylut+MJqh2gHuExdzxHFNtKH69BzlK7hDBjyyrLuHIxc4YZaxHGe5n\n" +
          "y3wF4QkEgfI+C5chH7Bi+jV694L40zEeFO2OhIif8Ti9bRb2Pk6UV5MrsdM0K6J0\n" +
          "gTQeTaRecQSg07vO3E8/GwfP2Dnq4yHICF/eaop+9QWj8UstEE6nEs7SSTrjIAxw\n" +
          "AeZzpkjkXPXTLjz6EcS/9EU7B+5v1qwXk1YeW1qerKJn6Qd6hqJ5gkVzq3sy3eOD\n" +
          "yrEwpNQoAR4J8e3VQkKOn9oiAlFTglFeBhe5AQ0EXAFY4QEH/2dyWbH3y9+hKk9R\n" +
          "xwFzO+5nGaqT6Njoh368GEEWgSG11NKlrD8k2y1/R1Nc3xEIWMHSUe1rnWWVONKh\n" +
          "upwXABTnj8coM5beoxVu9p1oYgum4IwLF0yAtaWll1hjsECm/U33Ok36JDa0iu+d\n" +
          "RDfXbEo5cX9bzc1QnWdM5tBg2mxRkssbY3eTPXUe4FLcT0WAQ5hjLW0tPneGzlu2\n" +
          "q9DkmngjDlwGgGhMCa/508wMpgGugE/C4V41EiiTAtOtVzGtdqPGVdoZeaYZLc9n\n" +
          "TQderaDu8oipaWIwsshYWX4uVVvo7xsx5c5PWXRdI70aUs5IwMRzuljbq+SYCNta\n" +
          "/uJRYc0AEQEAAYkBHwQYAQgAEwUCXAFY4QkQOjD0zAqajxACGwwAAI03B/9aWF8l\n" +
          "1v66Qaw4O8P3VyQn0/PkVWJYVt5KjMW4nexAfM4BlUw697rP5IvfYXNh47Cm8VKq\n" +
          "xgcXodzJrouzgwiPFxXmJe5Ug24FOpmeSeIl83UfCzaiIm+B6K5cf2NuHTrr4pEl\n" +
          "DaQ7RQGH2m2cMcimv4oWU9a0tRjt1e7XQAfQSWoCalUbLBeYORgVAF97MUNqeth6\n" +
          "FMT5STjq+AGgnNZ2vdsUnASS/HbQQUUOaVGVjo29lB6fS+UHT2gV/E/WQInjok5U\n" +
          "rUMaFHwpO0VNP057DNyqhZwxaAs5BsSgJlOC5hrT+PKlfr9ic75fqnJqmLircB+h\n" +
          "VnfhGR9OzH3RCIky\n" +
          "=qi5t\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n",
      users = listOf("Test <t@est.com>"),
      ids = listOf(
        KeyId(
          fingerprint = "E76853E128A0D376CAE47C143A30F4CC0A9A8F10"
        ),
        KeyId(
          fingerprint = "9EF2F8F36A841C0D5FAB8B0F0BAB9C018B265D22"
        )
      ),
      created = 1543592161000L,
      lastModified = 1543592161000L,
      algo = Algo(algorithm = "RSA_GENERAL", algorithmId = 1, bits = 2047, curve = null)
    )
    assertEquals(1, result.getAllKeys().size)
    assertEquals(expected, result.toPgpKeyDetailsList().first())
  }

  @Test
  fun testParseKeysWithExpiredKey() {
    val pubKey = TestKeys.KEYS["expired"]!!.publicKey
    val result = PgpKey.parseKeys(pubKey.toByteArray())

    // TODO update from output
    val expected = PgpKeyDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false, privateKey = null,
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "Version: FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "mQENBF8PcdUBCADi8no6T4Bd9Ny5COpbheBuPWEyDOedT2EVeaPrfutB1D8iCP6R\n" +
          "f1cUvs/qNUX/O7HQHFpgFuW2uOY4OU5cvcrwmNpOxT3pPt2cavxJMdJofwEvloY3\n" +
          "OfY7MCqdAj5VUcFGMhubfV810V2n5pf2FFUNTirksT6muhviMymyuWZLdh0F4Wxr\n" +
          "XEon7k3y2dZ3mI4xsG+Djttb6hj3gNr8/zNQQnTmVjB0mmpOFcGUQLTTTYMngvVM\n" +
          "kz8/sh38trqkVGuf/M81gkbr1egnfKfGz/4NT3qQLjinnA8In2cSFS/MipIV14gT\n" +
          "fHQAICFIMsWuW/xkaXUqygvAnyFa2nAQdgELABEBAAG0KDxhdXRvLnJlZnJlc2gu\n" +
          "ZXhwaXJlZC5rZXlAcmVjaXBpZW50LmNvbT6JAVMEEAEIACYFAl8PcdUFCQAAAAEG\n" +
          "CwkHCAMCBBUICgIEFgIBAAIZAQIbAwIeAQAhCRC+46QtmpyKyRYhBG0+CYZ1RO5i\n" +
          "fy6Sj77jpC2anIrJIvQIALG8TGMNYB4CRouMJawNCLui6Fx4Ba1ipPTaqlJPybLo\n" +
          "e6z/WVZwAA9CmbjkCIk683ppmGQ3GXv7f8Sdk7DqhEhfZ7JtAK/Uw2VZqqIryNrr\n" +
          "B0WV3EUHsENCOlq0YJodLqtkqgl83lCNDIkeoQwq4IyrgC8wsPgF7YMpxxQLONJv\n" +
          "ChZxSdCDjnfX3kvOZsLYFiKnNlX6wyrKAQxWnxxYhglMf0GDDyh0AJ+vOQHJ9m+o\n" +
          "eBnA1tJ5AZU5aQHvRtyWBKkYaEhljhyWr3eu1JjK4mn7/W6Rszveso33987wtIoQ\n" +
          "66GpGcX2mh7y217y/uXz4D3X5PUEBXIbhvAPty71bnS5AQ0EXw9x1QEIALdJgAsQ\n" +
          "0JnvLXwAKoOammWlUQmracK89v1Yc4mFnImtHDHS3pGsbx3DbNGuiz5BhXCdoPDf\n" +
          "gMxlGmJgShy9JAhrhWFXkvsjW/7aO4bM1wU486VPKXb7Av/dcrfHH0ASj4zj/TYA\n" +
          "eubNoxQtxHgyb13LVCW1kh4Oe6s0ac/hKtxogwEvNFY3x+4yfloHH0Ik9sbLGk0g\n" +
          "S03bPABDHMpYk346406f5TuP6UDzb9M90i2cFxbq26svyBzBZ0vYzfMRuNsm6an0\n" +
          "+B/wS6NLYBqsRyxwwCTdrhYS512yBzCHDYJJX0o3OJNe85/0TqEBO1prgkh3QMfw\n" +
          "13/Oxq8PuMsyJpUAEQEAAYkBPAQYAQgADwUCXw9x1QUJAAAAAQIbDAAhCRC+46Qt\n" +
          "mpyKyRYhBG0+CYZ1RO5ify6Sj77jpC2anIrJARgH/1KV7JBOS2ZEtO95FrLYnIqI\n" +
          "45rRpvT1XArpBPrYLuHtDBwgMcmpiMhhKIZCFlZkR1W88ENdSkr8Nx81nW+f9JWR\n" +
          "R6HuSyom7kOfS2Gdbfwo3bgp48DWr7K8KV/HHGuqLqd8UfPyDpsBGNx0w7tRo+8v\n" +
          "qUbhskquLAIahYCbhEIE8zgy0fBVhXKFe1FjuFUoW29iEm0tZWX0k2PT5r1owEgD\n" +
          "e0g/X1AXgSQyfPRFVDwE3QNJ1np/Rmygq1C+DIW2cohJOc7tO4gbl11XolsfQ+FU\n" +
          "+HewYXy8aAEbrTSRfsffMvK6tgT9BZ3kzjOxT5ou2SdvTa0eUk8k+zv8OnJJfXA=\n" +
          "=2D79\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n",
      users = listOf("<auto.refresh.expired.key@recipient.com>"),
      ids = listOf(
        KeyId(
          fingerprint = "6D3E09867544EE627F2E928FBEE3A42D9A9C8AC9"
        ),
        KeyId(
          fingerprint = "0731F9992FE2152E101E0D37D16EE86BDB129956"
        )
      ),
      created = 1594847701000L,
      lastModified = 1594847701000L,
      expiration = 1594847702000L,
      algo = Algo(algorithm = "RSA_GENERAL", algorithmId = 1, bits = 2048, curve = null)
    )
    assertEquals(1, result.getAllKeys().size)
    assertEquals(expected, result.toPgpKeyDetailsList().first())
  }
}
