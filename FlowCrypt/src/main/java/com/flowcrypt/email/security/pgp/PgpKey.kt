/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.pgp.armor
import com.flowcrypt.email.extensions.pgp.toPgpKeyDetails
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.key.collection.PGPKeyRingCollection
import org.pgpainless.util.Passphrase
import java.io.InputStream

@Suppress("unused")
object PgpKey {
  /**
   * Encrypt the given key.
   *
   * @param armored Should be a single private key.
   */
  fun encryptKey(armored: String, passphrase: Passphrase): String {
    return encryptKey(extractSecretKeyRing(armored), passphrase).armor()
  }

  /**
   * Decrypt the given key.
   *
   * @param armored Should be a single private key.
   */
  fun decryptKey(armored: String, passphrase: Passphrase): String {
    return decryptKey(extractSecretKeyRing(armored), passphrase).armor()
  }

  /**
   * Change a passphrase for the given key.
   *
   * @param armored Should be a single private key.
   */
  fun changeKeyPassphrase(armored: String,
                          oldPassphrase: Passphrase, newPassphrase: Passphrase): String {
    return changeKeyPassphrase(extractSecretKeyRing(armored), oldPassphrase, newPassphrase).armor()
  }

  fun parseKeys(source: String, throwExceptionIfUnknownSource: Boolean = true): ParseKeyResult {
    return parseKeys(source.toByteArray().inputStream(), throwExceptionIfUnknownSource)
  }

  fun parseKeys(source: ByteArray, throwExceptionIfUnknownSource: Boolean = true): ParseKeyResult {
    return parseKeys(source.inputStream(), throwExceptionIfUnknownSource)
  }

  /**
   * Parses multiple keys, binary or armored. It can take one key or many keys, it can be
   * private or public keys, it can be armored or binary... doesn't matter.
   * Cannot contain binary and armored at the same time.
   *
   * @return parsing result object
   */
  fun parseKeys(source: InputStream, throwExceptionIfUnknownSource: Boolean = true): ParseKeyResult {
    return ParseKeyResult(
        PGPainless.readKeyRing().keyRingCollection(source, throwExceptionIfUnknownSource))
  }

  private fun decryptKey(key: PGPSecretKeyRing, passphrase: Passphrase): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
        .changePassphraseFromOldPassphrase(passphrase)
        .withSecureDefaultSettings()
        .toNoPassphrase()
        .done()
  }

  private fun encryptKey(key: PGPSecretKeyRing, passphrase: Passphrase): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
        .changePassphraseFromOldPassphrase(null)
        .withSecureDefaultSettings()
        .toNewPassphrase(passphrase)
        .done()
  }

  private fun changeKeyPassphrase(
      key: PGPSecretKeyRing,
      oldPassphrase: Passphrase,
      newPassphrase: Passphrase
  ): PGPSecretKeyRing {
    return encryptKey(decryptKey(key, oldPassphrase), newPassphrase)
  }

  private fun extractSecretKeyRing(armored: String): PGPSecretKeyRing {
    val parseKeyResult = parseKeys(armored)
    if (parseKeyResult.getAllKeys().isEmpty()) {
      throw IllegalArgumentException("Keys not found")
    }
    if (parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.firstOrNull() is PGPSecretKeyRing)
      return parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.first()
    else
      throw IllegalArgumentException("Key is not a secret key")
  }

  data class ParseKeyResult(val pgpKeyRingCollection: PGPKeyRingCollection) {
    fun getAllKeys(): List<PGPKeyRing> =
        pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings.asSequence().toList() +
            pgpKeyRingCollection.pgpPublicKeyRingCollection.keyRings.asSequence().toList()

    fun toNodeKeyDetailsList() = getAllKeys().map { it.toPgpKeyDetails() }
  }
}
