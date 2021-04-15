/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import com.flowcrypt.email.extensions.kotlin.toHex
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.pgpainless.PGPainless
import org.pgpainless.key.util.KeyRingUtils
import org.pgpainless.key.util.SignatureUtils
import org.pgpainless.util.Passphrase
import java.lang.IllegalStateException

fun PGPSecretKeyRing.toPublicKeyRing(): PGPPublicKeyRing {
  return KeyRingUtils.publicKeyRingFrom(this)
}

fun PGPSecretKeyRing.isFullyDecrypted(): Boolean {
  return this.all { it.isDecrypted() }
}

fun PGPSecretKeyRing.isFullyEncrypted(): Boolean {
  return this.all { it.isEncrypted() }
}

fun PGPSecretKeyRing.isDecryptedFor(keyId: Long?): Boolean {
  val keyStats = countAllAndDecrypted()

  // fully decrypted
  if (keyStats.second == keyStats.first) return true

  // fully encrypted or key id not specified, so don't know
  if (keyStats.second == 0 || keyId == null) return false

  val key = firstOrNull { it.keyID == keyId }
  return key?.isDecrypted() ?: false
}

fun PGPSecretKeyRing.countAllAndDecrypted(): Pair<Int, Int> {
  var totalCount = 0
  var decryptedCount = 0
  for (key in this) {
    ++totalCount
    if (key.isDecrypted()) ++decryptedCount
  }
  return Pair(totalCount, decryptedCount)
}

fun PGPSecretKeyRing.decrypt(passphrase: CharArray): PGPSecretKeyRing {
  return PGPainless.modifyKeyRing(this)
      .changePassphraseFromOldPassphrase(Passphrase(passphrase))
      .withSecureDefaultSettings()
      .toNoPassphrase()
      .done()
}

fun PGPSecretKeyRing.encrypt(passphrase: CharArray): PGPSecretKeyRing {
  return PGPainless.modifyKeyRing(this)
      .changePassphraseFromOldPassphrase(null)
      .withSecureDefaultSettings()
      .toNewPassphrase(Passphrase(passphrase))
      .done()
}

fun PGPSecretKeyRing.changePassphrase(
    oldPassphrase: CharArray,
    newPassphrase: CharArray
): PGPSecretKeyRing {
  return this.decrypt(oldPassphrase).encrypt(newPassphrase)
}

fun PGPSecretKeyRing.getDecryptionKeys(
    keyId: Long? = null,
    userId: String? = null
): List<PGPSecretKey> {
  val publicKeyRing = toPublicKeyRing()
  return this.filter { currentSecretKey ->
    val currentPublicKey = currentSecretKey.publicKey
    if (keyId == null || currentSecretKey.keyID == keyId) {
      if (currentSecretKey === this.secretKey) {
        val primaryUser = publicKeyRing.getPrimaryUser()
        return@filter primaryUser.selfCertification.isValidDecryptionKeyUnchecked()
      } else {
        val binding = currentPublicKey.getLatestValidSignature(
            publicKeyRing = publicKeyRing,
            signatureType = PGPSignature.SUBKEY_BINDING,
            userId = userId
        )
        return@filter binding != null && binding.isValidDecryptionKeyUnchecked()
      }
    } else {
      return@filter false
    }
  }.toList()
}
