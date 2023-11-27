/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.extensions.kotlin.normalize
import org.junit.Assert.assertEquals
import org.junit.Test

class RawBlockParserTest {
  @Test
  fun testNoStringIndexOutOfBoundsExceptionInParser() {
    checkForSinglePlaintextBlock("-----BEGIN FOO-----\n")
  }

  @Test
  fun testDetectBlocksDoesNotGetTrippedOnBlocksWithUnknownHeaders() {
    checkForSinglePlaintextBlock(
      "This text breaks email and Gmail web app.\n\n" +
          "-----BEGIN FOO-----\n\n" +
          "Even though it's not a valid PGP m\n\n" +
          "Muhahah"
    )
  }

  @Test
  fun testDetectBlocksIgnoresFalsePositiveBlocks() {
    checkForSinglePlaintextBlock(
      """Hello, sending you the promised json:
      {
        "entries" : [ {
          "id" : "1,email-key-manager,evaluation.org,pgp-key-private,106988520142055188323",
          "content" : "-----BEGIN PGP PRIVATE KEY BLOCK-----
Version: FlowCrypt Email Encryption 7.6.9
Comment: Seamlessly send and receive encrypted email

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
+P9DgunMb5OtkDwm6WWxpzV150LJcA==
=FAco
-----END PGP PRIVATE KEY BLOCK-----
"
        }, {
          "id" : "1,email-key-manager,evaluation.org,pgp-key-public,ekm%40ekm-org-rules-test.flowcrypt.com",
          "content" : "-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: FlowCrypt Email Encryption 7.6.9
Comment: Seamlessly send and receive encrypted email

xsBNBF5mRKEBCADX62s0p6mI6yrxB/ui/LqxfG4RcQzZJf8ah52Ynu1n8V7Y
7143LmT3MfCDw1bfHu2k1OK7hT+BOi6sXas1D/fVtjz5WwuoBvwf1DBZ7eq8
tMQbLqQ7m/A8uwrVFOhWfuxulM7RuzIPIgv4HqtKKEugprUd80bPus45+f80
H6ZSgEpmZD6t9JShY6f8pU1OHcnPqFsFF0sLyOk7WcCG5Li3WjkwU/lIu18q
R26oLb5UM8z6vv6JD29GmqCj+OLYaPk8b00kdpGEvTjw3VzGM+tXOgUf2y1T
K9UfhMNkyswxUZw543CMTdw9V0+AzM0q70T/p0fP9nlJCv6M3bQm6D/vABEB
AAHNL0VrbSBVc2VyIDxla21AZWttLW9yZy1ydWxlcy10ZXN0LmZsb3djcnlw
dC5jb20+wsB1BBABCAAfBQJeZkShBgsJBwgDAgQVCAoCAxYCAQIZAQIbAwIe
AQAKCRDESadeBea4P0KvCACD5uOgGxwGEmUWfH8EXPK7npDKulmoZnSWYrfC
X3ctUKXjwPBWRXYid7LChnQAR6SRcyxyD1Eoel5ZVrJyKHqRkxcanFHeqRU1
OyOgtsQyPIGtLipmOgc6i5JYhqbQ4mNu10CGS6ZKhjf6rFIqLl/8f4lnBc28
UqVuP20Ru6KJZTVVQRF28FweMByR/3LyAWfObMwXJ0+uFEV941VEDv5MGdId
fePTP2cHRSJxPqVhpPWtfzYLStUzLFvtLfE45hympok4lZeKfLVtZVVQEgT+
ojEImdiZQJ0dT+jeJhmuTjzURQcLapXv2GLBUZaY2zfoAXR31QNYjADOxlrO
utSUzsBNBF5mRKEBCACVNQTzI2Cf1+G3q38OtXO89tuBI/a5TjcHh/sFIJB6
PPuEg/uW+EsjkgI3yk+UZZd6iYohO2mJcJ7MnaFHOu7tmOEaaHSiYsA0RTnV
qUBlbHbsl2oSlQJ/mjJ4cWq5ateuLHhx2RV0t1bm2anHJnqKGkqYqXA72m5g
rLzRSJ9M43wQRheGWGNoNdg4kPxU+PjYwfk2ARX5SCUKoG0qp0RhRMplX74u
Yi+Ek/9qSyZevmhK55sXIUNwLsuEhejlr0iucOt2vcIybQ9EbMXz62yYMRjY
gy4SxW5aQJxXFeWkSo6wzMqQ1ZiSArRCezBk+mftxNrmwmtCcJajQt2uAQQV
ABEBAAHCwF8EGAEIAAkFAl5mRKECGwwACgkQxEmnXgXmuD/7VAf+IMJMoADc
dWNhn45AvkwbzSmYt4i2aRGe+qojswwYzvFBFZtyZ/FKV2+LHfKUBI18FRmH
mKEba1UUetflytxiAwZxSJSf7Yz/NDiWaVn0eOLopmFMiPb02a5i3CjbLsDe
ex2y/69R0+fQc+rE3HZ04C8H/YAqFV0VOv3L+2EztOGK7KOZOx4toR05oDqb
ZbiDzwhsa2MugHLPLZuGl3eGk+n/EcINhopHg+HU8MHQE6rADvrok6QiYVhp
Gqi8ksD3kBAk43hGRSD2m/WDPWa/h2sh5rVswTKUDtv1fd1H6Ff5FnK21LHj
Ek0f+P9DgunMb5OtkDwm6WWxpzV150LJcA==
=Hcoc
-----END PGP PUBLIC KEY BLOCK-----
"
        }, {
          "id" : "1,email-key-manager,evaluation.org,pgp-key-fingerprint,C05803F40E0B9FE4FE9B4822C449A75E05E6B83F",
          "content" : "1,email-key-manager,evaluation.org,pgp-key-private,106988520142055188323
1,email-key-manager,evaluation.org,pgp-key-public,ekm%40ekm-org-rules-test.flowcrypt.com"
        } ]
      }"""
    )
  }

  @Test
  fun testDetectBlocksReplacesIntendedBlocks() {
    val prv = """-----BEGIN PGP PRIVATE KEY BLOCK-----
Version: FlowCrypt Email Encryption 7.6.9
Comment: Seamlessly send and receive encrypted email

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
+P9DgunMb5OtkDwm6WWxpzV150LJcA==
=FAco
-----END PGP PRIVATE KEY BLOCK-----"""

    val pub = """-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: FlowCrypt Email Encryption 7.6.9
Comment: Seamlessly send and receive encrypted email

xsBNBF5mRKEBCADX62s0p6mI6yrxB/ui/LqxfG4RcQzZJf8ah52Ynu1n8V7Y
7143LmT3MfCDw1bfHu2k1OK7hT+BOi6sXas1D/fVtjz5WwuoBvwf1DBZ7eq8
tMQbLqQ7m/A8uwrVFOhWfuxulM7RuzIPIgv4HqtKKEugprUd80bPus45+f80
H6ZSgEpmZD6t9JShY6f8pU1OHcnPqFsFF0sLyOk7WcCG5Li3WjkwU/lIu18q
R26oLb5UM8z6vv6JD29GmqCj+OLYaPk8b00kdpGEvTjw3VzGM+tXOgUf2y1T
K9UfhMNkyswxUZw543CMTdw9V0+AzM0q70T/p0fP9nlJCv6M3bQm6D/vABEB
AAHNL0VrbSBVc2VyIDxla21AZWttLW9yZy1ydWxlcy10ZXN0LmZsb3djcnlw
dC5jb20+wsB1BBABCAAfBQJeZkShBgsJBwgDAgQVCAoCAxYCAQIZAQIbAwIe
AQAKCRDESadeBea4P0KvCACD5uOgGxwGEmUWfH8EXPK7npDKulmoZnSWYrfC
X3ctUKXjwPBWRXYid7LChnQAR6SRcyxyD1Eoel5ZVrJyKHqRkxcanFHeqRU1
OyOgtsQyPIGtLipmOgc6i5JYhqbQ4mNu10CGS6ZKhjf6rFIqLl/8f4lnBc28
UqVuP20Ru6KJZTVVQRF28FweMByR/3LyAWfObMwXJ0+uFEV941VEDv5MGdId
fePTP2cHRSJxPqVhpPWtfzYLStUzLFvtLfE45hympok4lZeKfLVtZVVQEgT+
ojEImdiZQJ0dT+jeJhmuTjzURQcLapXv2GLBUZaY2zfoAXR31QNYjADOxlrO
utSUzsBNBF5mRKEBCACVNQTzI2Cf1+G3q38OtXO89tuBI/a5TjcHh/sFIJB6
PPuEg/uW+EsjkgI3yk+UZZd6iYohO2mJcJ7MnaFHOu7tmOEaaHSiYsA0RTnV
qUBlbHbsl2oSlQJ/mjJ4cWq5ateuLHhx2RV0t1bm2anHJnqKGkqYqXA72m5g
rLzRSJ9M43wQRheGWGNoNdg4kPxU+PjYwfk2ARX5SCUKoG0qp0RhRMplX74u
Yi+Ek/9qSyZevmhK55sXIUNwLsuEhejlr0iucOt2vcIybQ9EbMXz62yYMRjY
gy4SxW5aQJxXFeWkSo6wzMqQ1ZiSArRCezBk+mftxNrmwmtCcJajQt2uAQQV
ABEBAAHCwF8EGAEIAAkFAl5mRKECGwwACgkQxEmnXgXmuD/7VAf+IMJMoADc
dWNhn45AvkwbzSmYt4i2aRGe+qojswwYzvFBFZtyZ/FKV2+LHfKUBI18FRmH
mKEba1UUetflytxiAwZxSJSf7Yz/NDiWaVn0eOLopmFMiPb02a5i3CjbLsDe
ex2y/69R0+fQc+rE3HZ04C8H/YAqFV0VOv3L+2EztOGK7KOZOx4toR05oDqb
ZbiDzwhsa2MugHLPLZuGl3eGk+n/EcINhopHg+HU8MHQE6rADvrok6QiYVhp
Gqi8ksD3kBAk43hGRSD2m/WDPWa/h2sh5rVswTKUDtv1fd1H6Ff5FnK21LHj
Ek0f+P9DgunMb5OtkDwm6WWxpzV150LJcA==
=Hcoc
-----END PGP PUBLIC KEY BLOCK-----"""
    val input = "Hello, these should get replaced:\n$prv\n\nAnd this one too:\n\n$pub"
    assertEquals(input, input.normalize())

    val blocks = RawBlockParser.detectBlocks(input).toList()

    assertEquals(4, blocks.size)

    assertEquals(
      RawBlockParser.RawBlock(
        RawBlockParser.RawBlockType.PLAIN_TEXT,
        "Hello, these should get replaced:".toByteArray(),
        false
      ),
      blocks[0]
    )

    assertEquals(
      RawBlockParser.RawBlock(
        RawBlockParser.RawBlockType.PGP_PRIVATE_KEY,
        prv.trimEnd().toByteArray(),
        false
      ),
      blocks[1]
    )

    assertEquals(
      RawBlockParser.RawBlock(
        RawBlockParser.RawBlockType.PLAIN_TEXT,
        "And this one too:".trimEnd().toByteArray(),
        false
      ),
      blocks[2]
    )

    assertEquals(
      RawBlockParser.RawBlock(
        RawBlockParser.RawBlockType.PGP_PUBLIC_KEY,
        pub.trimEnd().toByteArray(),
        false
      ),
      blocks[3]
    )
  }

  private fun checkForSinglePlaintextBlock(input: String) {
    assertEquals(input, input.normalize())
    val blocks = RawBlockParser.detectBlocks(input)
    assertEquals(1, blocks.size)
    val expectedBlock = RawBlockParser.RawBlock(
      RawBlockParser.RawBlockType.PLAIN_TEXT,
      input.trimEnd().toByteArray(),
      false
    )
    assertEquals(expectedBlock, blocks.first())
  }
}
