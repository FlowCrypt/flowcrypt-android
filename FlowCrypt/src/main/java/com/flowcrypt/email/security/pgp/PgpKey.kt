/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.extensions.pgp.armor
import com.flowcrypt.email.extensions.pgp.toNodeKeyDetails
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.pgpainless.PGPainless
import org.pgpainless.key.collection.PGPKeyRingCollection
import org.pgpainless.util.Passphrase
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Suppress("unused")
object PgpKey {
  fun encryptKey(armored: String, passphrase: String): String {
    return encryptKey(extractSecretKey(armored), passphrase).armor()
  }

  private fun encryptKey(key: PGPSecretKeyRing, passphrase: String): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
        .changePassphraseFromOldPassphrase(null)
        .withSecureDefaultSettings()
        .toNewPassphrase(Passphrase.fromPassword(passphrase))
        .done()
  }

  fun decryptKey(armored: String, passphrase: String): String {
    return decryptKey(extractSecretKey(armored), passphrase).armor()
  }

  private fun decryptKey(key: PGPSecretKeyRing, passphrase: String): PGPSecretKeyRing {
    return PGPainless.modifyKeyRing(key)
        .changePassphraseFromOldPassphrase(Passphrase.fromPassword(passphrase))
        .withSecureDefaultSettings()
        .toNoPassphrase()
        .done()
  }

  fun changeKeyPassphrase(
      armored: String,
      oldPassphrase: String,
      newPassphrase: String
  ): String {
    return changeKeyPassphrase(extractSecretKey(armored), oldPassphrase, newPassphrase).armor()
  }

  private fun changeKeyPassphrase(
      key: PGPSecretKeyRing,
      oldPassphrase: String,
      newPassphrase: String
  ): PGPSecretKeyRing {
    return encryptKey(decryptKey(key, oldPassphrase), newPassphrase)
  }

  private fun extractSecretKey(armored: String): PGPSecretKeyRing {
    val key = parseAndNormalizeKeyRings(armored).firstOrNull()
        ?: throw IllegalArgumentException("Keys not found")
    if (key is PGPSecretKeyRing)
      return key
    else
      throw IllegalArgumentException("Key is not a secret key")
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
    return ParseKeyResult(PGPainless.readKeyRing().keyRingCollection(source, throwExceptionIfUnknownSource))
  }

  private fun parseAndNormalizeKeyRings(armored: String): List<PGPKeyRing> {
    val normalizedArmored = PgpArmor.normalize(armored, MsgBlock.Type.UNKNOWN)
    val keys = mutableListOf<PGPKeyRing>()
    if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.PUBLIC_KEY]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val keyRingCollection = JcaPGPPublicKeyRingCollection(
          ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream())
      )
      // We have to use reflection because BouncyCastle declares "order" list as a private field
      // https://stackoverflow.com/a/1196207/1540501
      // Sent a request to BouncyCastle mailing list to make this possible in a better way.
      val orderField = keyRingCollection.javaClass.superclass!!.getDeclaredField("order")
      orderField.isAccessible = true
      keys.addAll(
          (orderField.get(keyRingCollection) as java.util.List<java.lang.Long>).map {
            keyRingCollection.getPublicKeyRing(it.toLong())!!
          }
      )
    } else if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.PRIVATE_KEY]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val keyRingCollection = JcaPGPSecretKeyRingCollection(
          ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream())
      )
      // Again, use reflection because BouncyCastle declares "order" list as a private field.
      val orderField = keyRingCollection.javaClass.superclass!!.getDeclaredField("order")
      orderField.isAccessible = true
      keys.addAll(
          (orderField.get(keyRingCollection) as java.util.List<java.lang.Long>).map {
            keyRingCollection.getSecretKeyRing(it.toLong())!!
          }
      )
    } else if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.ENCRYPTED_MSG]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val objectFactory = PGPObjectFactory(
          ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream()),
          JcaKeyFingerprintCalculator()
      )
      while (true) {
        val obj = objectFactory.nextObject() ?: break
        if (obj is PGPKeyRing) {
          keys.add(obj)
        }
      }
    }

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

  data class ParseKeyResult(val pgpKeyRingCollection: PGPKeyRingCollection) {
    fun getAllKeys(): List<PGPKeyRing> =
        pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings.asSequence().toList() +
            pgpKeyRingCollection.pgpPublicKeyRingCollection.keyRings.asSequence().toList()

    fun toNodeKeyDetailsList() = getAllKeys().map { it.toNodeKeyDetails() }
  }
}
