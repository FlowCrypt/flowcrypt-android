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
import org.junit.Ignore
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.HashAlgorithm
import org.pgpainless.algorithm.KeyFlag
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.policy.Policy
import org.pgpainless.policy.Policy.HashAlgorithmPolicy
import org.pgpainless.util.Passphrase

class PgpKeyTest {
  companion object {
    @Suppress("SameParameterValue")
    private fun loadSecretKey(keyFile: String): PGPSecretKeyRing? {
      return PGPainless.getInstance().readKey().parseKey(
        (TestUtil.readResourceAsString("pgp/keys/$keyFile"))
      ).pgpSecretKeyRing
    }

    @Suppress("SameParameterValue")
    private fun loadPublicKey(keyFile: String): PGPPublicKeyRing? {
      return PGPainless.getInstance().readKey()
        .parseCertificate((TestUtil.readResourceAsString("pgp/keys/$keyFile"))).pgpPublicKeyRing
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
      publicKey = TestUtil.readResourceAsString(
        "pgp/keys/E76853E128A0D376CAE47C143A30F4CC0A9A8F10.public.gpg-key"
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
    val actual = parseKeyResult.pgpKeyDetailsList.first().copy(
      /*
       * we replace publicKey source here because it can't be a constant.
       * If other fields of [PgpKeyRingDetails] will be the same it means objects are equal
       */
      publicKey = expected.publicKey
    )
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
      publicKey = TestUtil.readResourceAsString(
        "pgp/keys/6D3E09867544EE627F2E928FBEE3A42D9A9C8AC9.public.gpg-key"
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
    val actual = parseKeyResult.pgpKeyDetailsList.first().copy(
      /*
       * we replace publicKey source here because it can't be a constant.
       * If other fields of [PgpKeyRingDetails] will be the same it means objects are equal
       */
      publicKey = expected.publicKey
    )
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
  @Ignore("temporary disabled due to https://github.com/pgpainless/pgpainless/issues/488")
  fun testPublicKey_Issue1358() {
    val keyText = TestUtil.readResourceAsString("pgp/keys/issue-1358.public.gpg-key")
    val actual = PgpKey.parseKeys(source = keyText)
    assertEquals(1, actual.getAllKeys().size)
  }

  @Test
  fun testReadCorruptedPrivateKey() {
    try {
      PGPainless.getInstance().algorithmPolicy.enableKeyParameterValidation = true
      val encryptedKeyText =
        TestUtil.readResourceAsString("pgp/keys/issue-1669-corrupted.private.gpg-key")
      val passphrase = Passphrase.fromPassword("123")
      assertThrows(KeyIntegrityException::class.java) {
        PgpKey.checkSecretKeyIntegrity(encryptedKeyText, passphrase)
      }
    } finally {
      PGPainless.getInstance().algorithmPolicy.enableKeyParameterValidation = false
    }
  }

  @Test
  fun testRejectingSHA1KeysForModifiedPGPainlessPolicy() {
    val originalPolicy = PGPainless.getInstance().algorithmPolicy
    try {
      val keyWithSHA1Algo = TestUtil.readResourceAsString("pgp/keys/sha1@flowcrypt.test_pub.asc")
      val static2022SignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022SignatureHashAlgorithmPolicy()
      PGPainless.setInstance(
        PGPainless(
          algorithmPolicy = Policy.Builder(originalPolicy)
            .withDataSignatureHashAlgorithmPolicy(static2022SignatureHashAlgorithmPolicy)
            .withCertificationSignatureHashAlgorithmPolicy(static2022SignatureHashAlgorithmPolicy)
            .build()
        )
      )
      val currentPolicy = PGPainless.getInstance().algorithmPolicy
      assertFalse(currentPolicy.certificationSignatureHashAlgorithmPolicy.isAcceptable(HashAlgorithm.SHA1))
      assertThrows(PGPException::class.java) {
        PgpKey.parseKeys(source = keyWithSHA1Algo).pgpKeyDetailsList
      }
    } finally {
      PGPainless.setInstance(PGPainless(algorithmPolicy = originalPolicy))
    }
  }

  @Test
  fun testAcceptingSHA1KeysForModifiedPGPainlessPolicy() {
    val originalPolicy = PGPainless.getInstance().algorithmPolicy
    try {
      assertFalse(
        originalPolicy.certificationSignatureHashAlgorithmPolicy.isAcceptable(HashAlgorithm.SHA1)
      )
      val keyWithSHA1Algo = TestUtil.readResourceAsString("pgp/keys/sha1@flowcrypt.test_pub.asc")
      val algorithmPolicy = HashAlgorithmPolicy.static2022RevocationSignatureHashAlgorithmPolicy()
      PGPainless.setInstance(
        PGPainless(
          algorithmPolicy = Policy.Builder(originalPolicy)
            .withDataSignatureHashAlgorithmPolicy(algorithmPolicy)
            .withCertificationSignatureHashAlgorithmPolicy(
              algorithmPolicy
            )
            .build()
        )
      )

      assertTrue(
        PGPainless.getInstance().algorithmPolicy.certificationSignatureHashAlgorithmPolicy.isAcceptable(
          HashAlgorithm.SHA1
        )
      )
      val parseKeyResult = PgpKey.parseKeys(source = keyWithSHA1Algo).pgpKeyDetailsList
      assertEquals(1, parseKeyResult.size)
      assertEquals("5DE92AB364B3100D89FBF460241512660BDDC426", parseKeyResult.first().fingerprint)
    } finally {
      PGPainless.setInstance(PGPainless(algorithmPolicy = originalPolicy))
    }
  }
}
