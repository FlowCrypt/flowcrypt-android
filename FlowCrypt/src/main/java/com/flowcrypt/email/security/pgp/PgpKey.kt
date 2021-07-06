/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
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
  fun changeKeyPassphrase(
    armored: String,
    oldPassphrase: Passphrase, newPassphrase: Passphrase
  ): String {
    return changeKeyPassphrase(extractSecretKeyRing(armored), oldPassphrase, newPassphrase).armor()
  }

  fun parseKeys(source: String, throwExceptionIfUnknownSource: Boolean = true): ParseKeyResult {
    return parseKeys(source.toInputStream(), throwExceptionIfUnknownSource)
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

  fun decryptKey(key: PGPSecretKeyRing, passphrase: Passphrase): PGPSecretKeyRing {
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

  fun extractSecretKeyRing(armored: String): PGPSecretKeyRing {
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

    fun toPgpKeyDetailsList() = getAllKeys().map { it.toPgpKeyDetails() }
  }

  // Restored here some previous code. Not sure if PGPainless can help with this.
  fun parseAndNormalizeKeyRings(armored: String): List<PGPKeyRing> {
    val normalizedArmored = PgpArmor.normalize(armored, MsgBlock.Type.UNKNOWN)
    val keys = parseKeys(normalizedArmored, false).getAllKeys().toMutableList()

    // Prevent key bloat by removing all non-self certifications
    for ((keyRingIndex, keyRing) in keys.withIndex()) {
      val primaryKeyID = keyRing.publicKey.keyID
      if (keyRing is PGPPublicKeyRing) {
        var replacementKeyRing: PGPPublicKeyRing = keyRing
        for (publicKey in keyRing.publicKeys) {
          var replacementKey = publicKey
          for (sig in publicKey.signatures.asSequence().map { it as PGPSignature }.filter {
            it.isCertification && it.keyID != primaryKeyID
          }) {
            replacementKey = PGPPublicKey.removeCertification(replacementKey, sig)
          }
          if (replacementKey !== publicKey) {
            replacementKeyRing = PGPPublicKeyRing.insertPublicKey(
              replacementKeyRing, replacementKey
            )
          }
        }
        if (replacementKeyRing !== keyRing) {
          keys[keyRingIndex] = replacementKeyRing
        }
      } else if (keyRing is PGPSecretKeyRing) {
        var replacementKeyRing: PGPSecretKeyRing = keyRing
        for (secretKey in keyRing.secretKeys) {
          val publicKey = secretKey.publicKey
          var replacementPublicKey = publicKey
          for (sig in publicKey.signatures.asSequence().map { it as PGPSignature }.filter {
            it.isCertification && it.keyID != primaryKeyID
          }) {
            replacementPublicKey = PGPPublicKey.removeCertification(replacementPublicKey, sig)
          }
          if (replacementPublicKey !== publicKey) {
            val replacementKey = PGPSecretKey.replacePublicKey(secretKey, replacementPublicKey)
            replacementKeyRing = PGPSecretKeyRing.insertSecretKey(
              replacementKeyRing, replacementKey
            )
          }
        }
        if (replacementKeyRing !== keyRing) {
          keys[keyRingIndex] = replacementKeyRing
        }
      }
    }
    return keys
  }
}
