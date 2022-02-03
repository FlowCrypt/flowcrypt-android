/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.security.model.Algo
import com.flowcrypt.email.security.model.KeyId
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.TestUtil
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.util.Passphrase
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class PgpKeyTest {
  companion object {
    private fun loadResourceAsString(
      path: String,
      charset: Charset = StandardCharsets.UTF_8
    ): String = TestUtil.readResourceAsString("${PgpKeyTest::class.simpleName}/$path", charset)

    @Suppress("SameParameterValue")
    private fun loadSecretKey(keyFile: String): PGPSecretKeyRing {
      return PGPainless.readKeyRing().secretKeyRing(loadResourceAsString("keys/$keyFile"))
    }

    @Suppress("SameParameterValue")
    private fun loadPublicKey(keyFile: String): PGPPublicKeyRing {
      return PGPainless.readKeyRing().publicKeyRing(loadResourceAsString("keys/$keyFile"))
    }
  }

  @Test
  fun testParseKeysWithNormalKey() {
    val expected = PgpKeyDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false,
      usableForEncryption = true,
      isRevoked = false,
      privateKey = null,
      publicKey = loadResourceAsString(
        "keys/E76853E128A0D376CAE47C143A30F4CC0A9A8F10.public.gpg-key"
      ).replace("@@VERSION_NAME@@", BuildConfig.VERSION_NAME),
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
    val actual = PgpKey.parseKeys(TestKeys.KEYS["rsa1"]!!.publicKey)
    assertEquals(1, actual.getAllKeys().size)
    assertEquals(expected, actual.pgpKeyDetailsList.first())
  }

  @Test
  fun testParseKeysWithExpiredKey() {
    val expected = PgpKeyDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false,
      isRevoked = false,
      usableForEncryption = true,
      privateKey = null,
      publicKey = loadResourceAsString(
        "keys/6D3E09867544EE627F2E928FBEE3A42D9A9C8AC9.public.gpg-key"
      ).replace("@@VERSION_NAME@@", BuildConfig.VERSION_NAME),
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
    val actual = PgpKey.parseKeys(TestKeys.KEYS["expired"]!!.publicKey)
    assertEquals(1, actual.getAllKeys().size)
    assertEquals(expected, actual.pgpKeyDetailsList.first())
  }

  @Test
  fun testParseAndDecryptKey_Issue1296() {
    val publicKeyRing = loadPublicKey("issue-1296-0xA96B4C55A800DB83.public.gpg-key")
    val expectedFingerprint = OpenPgpV4Fingerprint(publicKeyRing)
    val secretKeyRing = loadSecretKey("issue-1296-0xA96B4C55A800DB83.secret-subkeys.gpg-key")
    assertEquals(expectedFingerprint, OpenPgpV4Fingerprint(secretKeyRing.publicKey))
    val passphrase = Passphrase.fromPassword("password12345678")
    val decryptedSecretKeyRing = PgpKey.decryptKey(secretKeyRing, passphrase)
    assertEquals(expectedFingerprint, OpenPgpV4Fingerprint(decryptedSecretKeyRing.publicKey))
  }

  @Test
  fun testPublicKey_Issue1358() {
    val keyText = loadResourceAsString("keys/issue-1358.public.gpg-key")
    val actual = PgpKey.parseKeys(keyText)
    assertEquals(1, actual.getAllKeys().size)
  }

  @Test
  fun testReadCorruptedPrivateKey() {
    val encryptedKeyText = loadResourceAsString("keys/issue-1669-corrupted.private.gpg-key")
    val passphrase = Passphrase.fromPassword("123")
    assertThrows(KeyIntegrityException::class.java) {
      PgpKey.checkSecretKeyIntegrity(encryptedKeyText, passphrase)
    }
  }
}
