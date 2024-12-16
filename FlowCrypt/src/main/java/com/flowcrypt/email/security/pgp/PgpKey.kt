/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.pgpainless.PGPainless
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.key.collection.PGPKeyRingCollection
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.key.protection.UnlockSecretKey
import org.pgpainless.key.util.UserId
import org.pgpainless.util.Passphrase
import java.io.InputStream

@Suppress("unused")
object PgpKey {

  /**
   * Create a PGP key.
   *
   * @param email An email address that will be used for [org.pgpainless.key.util.UserId].
   * @param name A name that will be used for [org.pgpainless.key.util.UserId].
   * @param passphrase A passphrase that will be used as a private key pass.
   */
  suspend fun create(
    email: String,
    name: String? = null,
    passphrase: String
  ): PGPSecretKeyRing =
    withContext(Dispatchers.IO) {
      return@withContext PGPainless.generateKeyRing().simpleEcKeyRing(
        if (name != null) UserId.nameAndEmail(name, email) else UserId.onlyEmail(email),
        passphrase
      )
    }

  /**
   * Encrypt the given key.
   *
   * @param armored Should be a single private key.
   */
  suspend fun encryptKeySuspend(armored: String, passphrase: Passphrase): String =
    withContext(Dispatchers.IO) {
      return@withContext encryptKey(extractSecretKeyRing(armored), passphrase).armor()
    }

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

  fun parseKeys(
    source: String,
    throwExceptionIfUnknownSource: Boolean = true,
    skipErrors: Boolean = false
  ): ParseKeyResult {
    return parseKeys(
      source = source.toInputStream(),
      throwExceptionIfUnknownSource = throwExceptionIfUnknownSource,
      skipErrors = skipErrors
    )
  }

  fun parseKeys(
    source: ByteArray,
    throwExceptionIfUnknownSource: Boolean = true,
    hideArmorMeta: Boolean = false,
    skipErrors: Boolean = false
  ): ParseKeyResult {
    return parseKeys(
      source = source.inputStream(),
      throwExceptionIfUnknownSource = throwExceptionIfUnknownSource,
      hideArmorMeta = hideArmorMeta,
      skipErrors = skipErrors
    )
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
    throwExceptionIfUnknownSource: Boolean = true,
    hideArmorMeta: Boolean = false,
    skipErrors: Boolean = false
  ): ParseKeyResult {
    return ParseKeyResult(
      pgpKeyRingCollection = parseKeysRaw(source, throwExceptionIfUnknownSource),
      hideArmorMeta = hideArmorMeta,
      skipErrors = skipErrors
    )
  }

  fun parseKeysRaw(
    source: String,
    throwExceptionIfUnknownSource: Boolean = true
  ): PGPKeyRingCollection {
    return parseKeysRaw(source.toInputStream(), throwExceptionIfUnknownSource)
  }

  fun parseKeysRaw(
    source: InputStream,
    throwExceptionIfUnknownSource: Boolean = true
  ): PGPKeyRingCollection {
    return PGPainless.readKeyRing().keyRingCollection(source, throwExceptionIfUnknownSource)
  }

  fun decryptKey(key: PGPSecretKeyRing, passphrase: Passphrase): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
      .changePassphraseFromOldPassphrase(passphrase)
      .withSecureDefaultSettings()
      .toNoPassphrase()
      .done()
  }

  @Throws(KeyIntegrityException::class)
  fun checkSecretKeyIntegrity(armored: String, passphrase: Passphrase) {
    val collection = PGPainless.readKeyRing().keyRingCollection(armored, false)
    val secretKeyRings = collection.pgpSecretKeyRingCollection
    if (secretKeyRings.size() == 0) throw KeyIntegrityException()
    checkSecretKeyIntegrity(secretKeyRings, passphrase)
  }

  @Throws(KeyIntegrityException::class)
  fun checkSecretKeyIntegrity(
    secretKeyRings: PGPSecretKeyRingCollection,
    passphrase: Passphrase
  ) {
    for (keyRing in secretKeyRings) {
      val protector = SecretKeyRingProtector.unlockEachKeyWith(passphrase, keyRing)
      for (key in keyRing.secretKeys) {
        UnlockSecretKey.unlockSecretKey(key, protector)
      }
    }
  }

  suspend fun parsePrivateKeys(source: String): List<PgpKeyRingDetails> =
    withContext(Dispatchers.IO) {
      parseKeys(source = source, throwExceptionIfUnknownSource = false).pgpKeyRingCollection
        .pgpSecretKeyRingCollection.map { it.toPgpKeyRingDetails() }
    }

  private fun encryptKey(key: PGPSecretKeyRing, passphrase: Passphrase): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
      .changePassphraseFromOldPassphrase(Passphrase.emptyPassphrase())
      .withSecureDefaultSettings()
      .toNewPassphrase(passphrase)
      .done()
  }

  private fun changeKeyPassphrase(
    key: PGPSecretKeyRing,
    oldPassphrase: Passphrase,
    newPassphrase: Passphrase
  ): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
      .changePassphraseFromOldPassphrase(oldPassphrase)
      .withSecureDefaultSettings()
      .toNewPassphrase(newPassphrase)
      .done()
  }

  fun extractSecretKeyRing(armored: String): PGPSecretKeyRing {
    val parseKeyResult = parseKeys(source = armored)
    if (parseKeyResult.getAllKeys().isEmpty()) {
      throw IllegalArgumentException("Keys not found")
    }
    if (parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.firstOrNull() is PGPSecretKeyRing)
      return parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.first()
    else
      throw IllegalArgumentException("Key is not a secret key")
  }

  data class ParseKeyResult(
    val pgpKeyRingCollection: PGPKeyRingCollection,
    val hideArmorMeta: Boolean = false,
    val skipErrors: Boolean = false
  ) {
    fun getAllKeys(): List<PGPKeyRing> =
      pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings.asSequence().toList() +
          pgpKeyRingCollection.pgpPublicKeyRingCollection.keyRings.asSequence().toList()

    val pgpKeyDetailsList =
      getAllKeys().mapNotNull {
        try {
          it.toPgpKeyRingDetails(hideArmorMeta = hideArmorMeta)
        } catch (e: Exception) {
          e.printStackTrace()
          if (skipErrors) {
            null
          } else {
            throw e
          }
        }
      }
  }

  // Restored here some previous code. Not sure if PGPainless can help with this.
  fun parseAndNormalizeKeyRings(armored: String): List<PGPKeyRing> {
    val normalizedArmored = PgpArmor.normalize(armored, RawBlockParser.RawBlockType.UNKNOWN)
    val keys = parseKeys(source = normalizedArmored, throwExceptionIfUnknownSource = false)
      .getAllKeys()
      .toMutableList()

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
