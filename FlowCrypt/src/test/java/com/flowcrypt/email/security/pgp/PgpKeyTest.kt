/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.security.model.Algo
import com.flowcrypt.email.security.model.KeyId
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.util.TestUtil
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.HashAlgorithm
import org.pgpainless.algorithm.KeyFlag
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.policy.Policy.HashAlgorithmPolicy
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
    private fun loadSecretKey(keyFile: String): PGPSecretKeyRing? {
      return PGPainless.readKeyRing().secretKeyRing(loadResourceAsString("keys/$keyFile"))
    }

    @Suppress("SameParameterValue")
    private fun loadPublicKey(keyFile: String): PGPPublicKeyRing? {
      return PGPainless.readKeyRing().publicKeyRing(loadResourceAsString("keys/$keyFile"))
    }
  }

  @Test
  fun testParseKeysWithNormalKey() {
    val expected = PgpKeyRingDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false,
      usableForEncryption = true,
      usableForSigning = true,
      isRevoked = false,
      privateKey = null,
      publicKey = loadResourceAsString(
        "keys/E76853E128A0D376CAE47C143A30F4CC0A9A8F10.public.gpg-key"
      ).replace("@@VERSION_NAME@@", BuildConfig.VERSION_NAME),
      users = listOf("Test <t@est.com>"),
      primaryUserId = "Test <t@est.com>",
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
      algo = Algo(algorithm = "RSA_GENERAL", algorithmId = 1, bits = 2047, curve = null),
      primaryKeyId = 4193120410270338832,
      possibilities = setOf(
        KeyFlag.ENCRYPT_COMMS.flag,
        KeyFlag.ENCRYPT_STORAGE.flag,
        KeyFlag.SIGN_DATA.flag,
        KeyFlag.CERTIFY_OTHER.flag,
      )
    )
    val parseKeyResult = PgpKey.parseKeys(source = TestKeys.KEYS["rsa1"]!!.publicKey)
    assertEquals(1, parseKeyResult.getAllKeys().size)
    val actual = parseKeyResult.pgpKeyDetailsList.first()
      .run { this.copy(publicKey = replaceVersionInKey(this.publicKey)) }
    assertEquals(expected, actual)
  }

  @Test
  fun testParseKeysWithExpiredKey() {
    val expected = PgpKeyRingDetails(
      isFullyDecrypted = true,
      isFullyEncrypted = false,
      isRevoked = false,
      usableForEncryption = false,
      usableForSigning = false,
      privateKey = null,
      publicKey = loadResourceAsString(
        "keys/6D3E09867544EE627F2E928FBEE3A42D9A9C8AC9.public.gpg-key"
      ).replace("@@VERSION_NAME@@", BuildConfig.VERSION_NAME),
      users = listOf("<auto.refresh.expired.key@recipient.com>"),
      primaryUserId = "<auto.refresh.expired.key@recipient.com>",
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
      algo = Algo(algorithm = "RSA_GENERAL", algorithmId = 1, bits = 2048, curve = null),
      primaryKeyId = -4691725871015490871,
      possibilities = setOf(
        KeyFlag.ENCRYPT_COMMS.flag,
        KeyFlag.ENCRYPT_STORAGE.flag,
        KeyFlag.SIGN_DATA.flag,
        KeyFlag.CERTIFY_OTHER.flag,
      )
    )
    val parseKeyResult = PgpKey.parseKeys(source = TestKeys.KEYS["expired"]!!.publicKey)
    assertEquals(1, parseKeyResult.getAllKeys().size)
    val actual = parseKeyResult.pgpKeyDetailsList.first()
      .run { this.copy(publicKey = replaceVersionInKey(this.publicKey)) }
    assertEquals(expected, actual)
  }

  @Test
  fun testParseAndDecryptKey_Issue1296() {
    val publicKeyRing = loadPublicKey("issue-1296-0xA96B4C55A800DB83.public.gpg-key")
    val expectedFingerprint = OpenPgpV4Fingerprint(requireNotNull(publicKeyRing))
    val secretKeyRing = loadSecretKey("issue-1296-0xA96B4C55A800DB83.secret-subkeys.gpg-key")
    assertEquals(
      expectedFingerprint,
      OpenPgpV4Fingerprint(requireNotNull(secretKeyRing?.publicKey))
    )
    val passphrase = Passphrase.fromPassword("password12345678")
    val decryptedSecretKeyRing = PgpKey.decryptKey(requireNotNull(secretKeyRing), passphrase)
    assertEquals(expectedFingerprint, OpenPgpV4Fingerprint(decryptedSecretKeyRing.publicKey))
  }

  @Test
  fun testPublicKey_Issue1358() {
    val keyText = loadResourceAsString("keys/issue-1358.public.gpg-key")
    val actual = PgpKey.parseKeys(source = keyText)
    assertEquals(1, actual.getAllKeys().size)
  }

  @Test
  fun testReadCorruptedPrivateKey() {
    try {
      PGPainless.getPolicy().enableKeyParameterValidation = true
      val encryptedKeyText = loadResourceAsString("keys/issue-1669-corrupted.private.gpg-key")
      val passphrase = Passphrase.fromPassword("123")
      assertThrows(KeyIntegrityException::class.java) {
        PgpKey.checkSecretKeyIntegrity(encryptedKeyText, passphrase)
      }
    } finally {
      PGPainless.getPolicy().enableKeyParameterValidation = false
    }
  }

  @Test
  fun testRejectingSHA1KeysForDefaultPGPainlessPolicy() {
    val keyWithSHA1Algo = loadResourceAsString("keys/sha1@flowcrypt.test_pub.asc")
    val policy = PGPainless.getPolicy()
    val certificationSignatureHashAlgorithmPolicy: HashAlgorithmPolicy =
      policy.certificationSignatureHashAlgorithmPolicy
    assertFalse(certificationSignatureHashAlgorithmPolicy.isAcceptable(HashAlgorithm.SHA1))
    assertThrows(PGPException::class.java) {
      PgpKey.parseKeys(source = keyWithSHA1Algo).pgpKeyDetailsList
    }
  }

  @Test
  fun testAcceptingSHA1KeysForModifiedPGPainlessPolicy() {
    val policy = PGPainless.getPolicy()
    val originalSignatureHashAlgorithmPolicy = policy.certificationSignatureHashAlgorithmPolicy
    try {
      assertFalse(policy.certificationSignatureHashAlgorithmPolicy.isAcceptable(HashAlgorithm.SHA1))
      val keyWithSHA1Algo = loadResourceAsString("keys/sha1@flowcrypt.test_pub.asc")
      PGPainless.getPolicy().certificationSignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022RevocationSignatureHashAlgorithmPolicy()
      assertTrue(policy.certificationSignatureHashAlgorithmPolicy.isAcceptable(HashAlgorithm.SHA1))
      val parseKeyResult = PgpKey.parseKeys(source = keyWithSHA1Algo).pgpKeyDetailsList
      assertEquals(1, parseKeyResult.size)
      assertEquals("5DE92AB364B3100D89FBF460241512660BDDC426", parseKeyResult.first().fingerprint)
    } finally {
      PGPainless.getPolicy().certificationSignatureHashAlgorithmPolicy =
        originalSignatureHashAlgorithmPolicy
    }
  }

  fun replaceVersionInKey(key: String?): String {
    val regex =
      "^Version: FlowCrypt Email Encryption \\d*.\\d*.\\d*(_.*)?\$".toRegex(RegexOption.MULTILINE)
    val version = BuildConfig.VERSION_NAME
    val replacement = "Version: FlowCrypt Email Encryption $version"
    key?.let {
      return key.replaceFirst(regex, replacement)
    }
    return ""
  }
}
