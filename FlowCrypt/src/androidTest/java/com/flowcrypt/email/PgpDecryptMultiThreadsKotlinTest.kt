/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */
package com.flowcrypt.email

import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.util.concurrent.atomic.AtomicInteger

class PgpDecryptMultiThreadsKotlinTest {

  @Test
  fun testDecryptionInMultiThreads() {
    val numberOfThreads = 2
    val numberOfAttempts = 500
    val atomicInteger = AtomicInteger()

    runBlocking {
      useParallel(numberOfThreads) {
        doDecryption(atomicInteger, numberOfAttempts)
      }
    }

    Assert.assertEquals((numberOfThreads * numberOfAttempts).toLong(), atomicInteger.get().toLong())
  }

  private suspend fun useParallel(
    parallelCount: Int = 1,
    action: suspend () -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val steps = mutableListOf<Deferred<Unit>>()
    for (i in 0 until parallelCount) {
      steps.add(async { action.invoke() })
    }

    awaitAll(*steps.toTypedArray())
  }

  private fun doDecryption(atomicInteger: AtomicInteger, attemptsCount: Int) {
    val secretKeyRingProtector = SecretKeyRingProtector.unlockAnyKeyWith(
      Passphrase.fromPassword(
        PASSPHRASE
      )
    )
    val pgpPublicKeyRingCollection = PGPainless.readKeyRing().publicKeyRingCollection(
      SENDER_PUBLIC_KEY + "\n" + RECEIVER_PUBLIC_KEY
    )
    val secretKeyRingCollection = PGPainless.readKeyRing().secretKeyRingCollection(
      SENDER_PRIVATE_KEY
    )

    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = ENCRYPTED_TEXT.toInputStream(),
      publicKeys = pgpPublicKeyRingCollection,
      secretKeys = secretKeyRingCollection,
      protector = secretKeyRingProtector,
      attempts = attemptsCount
    ) {
      atomicInteger.addAndGet(1)
    }

    Assert.assertEquals(ORIGINAL_TEXT, decryptionResult.content.toString())
  }

  companion object {
    private const val SENDER_PRIVATE_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "lIYEYIq7phYJKwYBBAHaRw8BAQdAat45rrh+gvQwWwJw5eScq3Pdxt/8d+lWNVSm\n" +
        "kImXcRP+CQMCvWfx3mzDdd5g6c59LcPqADK0p70/7ZmTkp3ZC1YViTprg4tQt/PF\n" +
        "QJL+VPCG+BF9bWyFcfxKe+KAnXRTWml5O6xrv6ZkiNmAxoYyO1shzLQWZGVmYXVs\n" +
        "dEBmbG93Y3J5cHQudGVzdIh4BBMWCgAgBQJgirumAhsDBRYCAwEABAsJCAcFFQoJ\n" +
        "CAsCHgECGQEACgkQIl+AI8INCVcysgD/cu23M07rImuV5gIl98uOnSIR+QnHUD/M\n" +
        "I34b7iY/iTQBALMIsqO1PwYl2qKwmXb5lSoMj5SmnzRRE2RwAFW3AiMCnIsEYIq7\n" +
        "phIKKwYBBAGXVQEFAQEHQA8q7iPr+0OXqBGBSAL6WNDjzHuBsG7uiu5w8l/A6v8l\n" +
        "AwEIB/4JAwK9Z/HebMN13mCOF6Wy/9oZK4d0DW9cNLuQDeRVZejxT8oFMm7G8iGw\n" +
        "CGNjIWWcQSvctBZtHwgcMeplCW7tmzkD3Nq/ty50lCwQQd6gZSXMiHUEGBYKAB0F\n" +
        "AmCKu6YCGwwFFgIDAQAECwkIBwUVCgkICwIeAQAKCRAiX4Ajwg0JV+sbAQCv4LVM\n" +
        "0+AN54ivWa4vPRyYOfSQ1FqsipkYLJce+xwUeAD+LZpEVCypFtGWQVdeSJVxIHx3\n" +
        "k40IfHsK0fGgR+NrRAw=\n" +
        "=osuI\n" +
        "-----END PGP PRIVATE KEY BLOCK-----"
    private const val SENDER_PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "mDMEYIq7phYJKwYBBAHaRw8BAQdAat45rrh+gvQwWwJw5eScq3Pdxt/8d+lWNVSm\n" +
        "kImXcRO0FmRlZmF1bHRAZmxvd2NyeXB0LnRlc3SIeAQTFgoAIAUCYIq7pgIbAwUW\n" +
        "AgMBAAQLCQgHBRUKCQgLAh4BAhkBAAoJECJfgCPCDQlXMrIA/3LttzNO6yJrleYC\n" +
        "JffLjp0iEfkJx1A/zCN+G+4mP4k0AQCzCLKjtT8GJdqisJl2+ZUqDI+Upp80URNk\n" +
        "cABVtwIjArg4BGCKu6YSCisGAQQBl1UBBQEBB0APKu4j6/tDl6gRgUgC+ljQ48x7\n" +
        "gbBu7orucPJfwOr/JQMBCAeIdQQYFgoAHQUCYIq7pgIbDAUWAgMBAAQLCQgHBRUK\n" +
        "CQgLAh4BAAoJECJfgCPCDQlX6xsBAK/gtUzT4A3niK9Zri89HJg59JDUWqyKmRgs\n" +
        "lx77HBR4AP4tmkRULKkW0ZZBV15IlXEgfHeTjQh8ewrR8aBH42tEDA==\n" +
        "=kdDK\n" +
        "-----END PGP PUBLIC KEY BLOCK-----"
    private const val RECEIVER_PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "mDMEYIucWBYJKwYBBAHaRw8BAQdAew+8mzMWyf3+Pfy49qa60uKV6e5os7de4TdZ\n" +
        "ceAWUq+0F2RlbmJvbmQ3QGZsb3djcnlwdC50ZXN0iHgEExYKACAFAmCLnFgCGwMF\n" +
        "FgIDAQAECwkIBwUVCgkICwIeAQIZAQAKCRDDIInNavjWzm3JAQCgFgCEyD58iEa/\n" +
        "Rw/DYNoQNoZC1lhw1bxBiOcIbtkdBgEAsDFZu3TBavOMKI7KW+vfMBHtRVbkMNpv\n" +
        "unaAldoabgO4OARgi5xYEgorBgEEAZdVAQUBAQdAB1/Mrq5JGYim4KqGTSK4OESQ\n" +
        "UwPgK56q0yrkiU9WgyYDAQgHiHUEGBYKAB0FAmCLnFgCGwwFFgIDAQAECwkIBwUV\n" +
        "CgkICwIeAQAKCRDDIInNavjWzjMgAQCU+R1fItqdY6lt9jXUqipmXuqVaEFPwNA8\n" +
        "YJ1rIwDwVQEAyUc8162KWzA2iQB5akwLwNr/pLDDtOWwhLUkrBb3mAc=\n" +
        "=pXF6\n" +
        "-----END PGP PUBLIC KEY BLOCK-----"
    private const val ENCRYPTED_TEXT = "-----BEGIN PGP MESSAGE-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "hF4D16Pe22XLHvsSAQdAGnbsUSmPL/R4O24FqxHeD9oH5Fj6Ezut1mo8l0jU3S8w\n" +
        "9DDfnhxOcH8Zz+2PNVzkY/exM4iLQNkhio7f4iBV4krIZtMOEE4/2k+TdspAJzoI\n" +
        "hF4DTxRYvSK3u1MSAQdAPsETmZ27Dk9HFzLy/pd7ddXYqOkTJKk+fCcyfrhXXE4w\n" +
        "5aQ/hD9XtuuB7/KNLMOPcL+b6r6iiXw2Hh+pmhz6NTvgUYMjeKUitGaRXZcFBsro\n" +
        "0jsBimy6w2qk/a40HbcHRh/K+cZPqeN9rRAJU3pZoky0+wDNwpgFCB/GaCgZ69jV\n" +
        "p1k9Cdg9r5xsxpP3Tg==\n" +
        "=20Uz\n" +
        "-----END PGP MESSAGE-----"
    private const val PASSPHRASE = "android"
    private const val ORIGINAL_TEXT = "Some text"
  }
}
