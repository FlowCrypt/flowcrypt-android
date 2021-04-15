/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.changePassphrase
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.decrypt
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.encrypt
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toNodeKeyDetails
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.key.collection.PGPKeyRingCollection
import java.io.InputStream

@Suppress("unused")
object PgpKey {
  /**
   * Encrypt the given key.
   *
   * @param armored Should be a single private key.
   */
  fun encryptKey(armored: String, passphrase: CharArray): String {
    return extractSecretKeyRing(armored).encrypt(passphrase).armor()
  }

  /**
   * Decrypt the given key.
   *
   * @param armored Should be a single private key.
   */
  fun decryptKey(armored: String, passphrase: CharArray): String {
    return extractSecretKeyRing(armored).decrypt(passphrase).armor()
  }

  /**
   * Change a passphrase for the given key.
   *
   * @param armored Should be a single private key.
   */
  fun changeKeyPassphrase(
      armored: String,
      oldPassphrase: CharArray,
      newPassphrase: CharArray
  ): String {
    return extractSecretKeyRing(armored).changePassphrase(oldPassphrase, newPassphrase).armor()
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
  fun parseKeys(
      source: InputStream,
      throwExceptionIfUnknownSource: Boolean = true
  ): ParseKeyResult {
    return ParseKeyResult(
        PGPainless.readKeyRing().keyRingCollection(source, throwExceptionIfUnknownSource)
    )
  }

  private fun extractSecretKeyRing(armored: String): PGPSecretKeyRing {
    val parseKeyResult = parseKeys(armored)
    if (parseKeyResult.getAllKeys().isEmpty()) throw IllegalArgumentException("Keys not found")
    if (parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.firstOrNull()
            is PGPSecretKeyRing) {
      return parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.first()
    }
    throw IllegalArgumentException("Key is not a secret key")
  }

  data class ParseKeyResult(val pgpKeyRingCollection: PGPKeyRingCollection) {
    fun getAllKeys(): List<PGPKeyRing> =
        pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings.asSequence().toList() +
            pgpKeyRingCollection.pgpPublicKeyRingCollection.keyRings.asSequence().toList()

    fun toNodeKeyDetailsList() = getAllKeys().map { it.toNodeKeyDetails() }
  }
}
