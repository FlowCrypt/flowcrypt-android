/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: ivan
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import androidx.annotation.WorkerThread
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForEncryption
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForSigning
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.Algo
import com.flowcrypt.email.security.model.KeyId
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.algorithm.PublicKeyAlgorithm
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.generation.type.eddsa.EdDSACurve
import org.pgpainless.key.info.KeyInfo
import org.pgpainless.key.info.KeyRingInfo
import java.io.IOException
import java.time.Instant

/**
 * @author Denys Bondarenko
 */

/**
 * This method should be called out of the UI thread.
 *
 * @exception IOException
 */
@Throws(IOException::class)
@WorkerThread
fun PGPKeyRing.toPgpKeyRingDetails(hideArmorMeta: Boolean = false): PgpKeyRingDetails {
  val keyRingInfo = KeyRingInfo(this)

  val algo = Algo(
    algorithm = keyRingInfo.algorithm.name,
    algorithmId = keyRingInfo.algorithm.algorithmId,
    bits = if (keyRingInfo.publicKey.bitStrength != -1) keyRingInfo.publicKey.bitStrength else 0,
    curve = when (keyRingInfo.algorithm) {
      PublicKeyAlgorithm.ECDSA, PublicKeyAlgorithm.ECDH -> KeyInfo.getCurveName(publicKey)
      PublicKeyAlgorithm.EDDSA -> EdDSACurve._Ed25519.getName() // for EDDSA KeyInfo.getCurveName(publicKey) return null
      else -> null
    }
  )

  val keyIdList = publicKeys.iterator().asSequence().toList()
    .map {
      KeyId(fingerprint = OpenPgpV4Fingerprint(it).toString())
    }

  if (keyIdList.isEmpty()) {
    throw IllegalArgumentException("There are no fingerprints")
  }

  val privateKey = if (keyRingInfo.isSecretKey) armor(hideArmorMeta = hideArmorMeta) else null
  val publicKey = if (keyRingInfo.isSecretKey) {
    (this as PGPSecretKeyRing).toPublicKeyRing().armor(hideArmorMeta = hideArmorMeta)
  } else {
    armor(hideArmorMeta = hideArmorMeta)
  }

  return PgpKeyRingDetails(
    isFullyDecrypted = keyRingInfo.isFullyDecrypted,
    isFullyEncrypted = keyRingInfo.isFullyEncrypted,
    isRevoked = getPublicKey().hasRevocation(),
    usableForEncryption = keyRingInfo.usableForEncryption,
    usableForSigning = keyRingInfo.usableForSigning,
    privateKey = privateKey,
    publicKey = publicKey,
    users = keyRingInfo.userIds,
    primaryUserId = keyRingInfo.primaryUserId,
    ids = keyIdList,
    created = keyRingInfo.creationDate.time,
    lastModified = keyRingInfo.lastModified.time,
    expiration = keyRingInfo.primaryKeyExpirationDate?.time,
    algo = algo,
    primaryKeyId = keyRingInfo.keyId,
    possibilities = mutableSetOf<Int>().apply {
      addAll(
        keyRingInfo.publicKeys.flatMap { keyRingInfo.getKeyFlagsOf(it.keyID) }.toSet()
          .map { it.flag })
    }
  )
}

@Throws(IOException::class)
fun PGPKeyRing.armor(hideArmorMeta: Boolean = false): String =
  SecurityUtils.armor(hideArmorMeta) { this.encode(it) }

val PGPKeyRing.expiration: Instant?
  get() {
    val publicKey = this.publicKey
    return if (publicKey.validSeconds == 0L) {
      null
    } else {
      Instant.ofEpochMilli(publicKey.creationTime.time + publicKey.validSeconds * 1000)
    }
  }
