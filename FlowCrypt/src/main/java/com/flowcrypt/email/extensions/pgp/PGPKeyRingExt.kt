/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.Algo
import com.flowcrypt.email.api.retrofit.response.model.node.KeyId
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.security.pgp.PgpArmor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.algorithm.PublicKeyAlgorithm
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.generation.type.eddsa.EdDSACurve
import org.pgpainless.key.info.KeyInfo
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denis Bondarenko
 *         Date: 3/11/21
 *         Time: 10:08 AM
 *         E-mail: DenBond7@gmail.com
 */
fun PGPKeyRing.toNodeKeyDetails(): NodeKeyDetails {
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

  val ids = publicKeys.iterator().asSequence().toList()
      .map {
        val fingerprint = OpenPgpV4Fingerprint(it)
        KeyId(
            fingerprint = fingerprint.toString(),
            longId = fingerprint.takeLast(16).toString(),
            shortId = fingerprint.takeLast(8).toString(),
            keywords = ""//skipped as deprecated
        )
      }

  val privateKey = if (keyRingInfo.isSecretKey) this.armor(PgpArmor.FLOWCRYPT_HEADERS) else null
  val publicKey = if (keyRingInfo.isSecretKey) {
    (this as PGPSecretKeyRing).toPublicKeyRing().armor(PgpArmor.FLOWCRYPT_HEADERS)
  } else {
    this.armor(PgpArmor.FLOWCRYPT_HEADERS)
  }

  return NodeKeyDetails(
      isFullyDecrypted = keyRingInfo.isFullyDecrypted,
      isFullyEncrypted = keyRingInfo.isFullyEncrypted(),
      privateKey = privateKey,
      publicKey = publicKey,
      users = keyRingInfo.userIds,
      ids = ids,
      created = TimeUnit.SECONDS.convert(keyRingInfo.creationDate.time, TimeUnit.MILLISECONDS),
      lastModified = TimeUnit.SECONDS.convert(keyRingInfo.lastModified.time, TimeUnit.MILLISECONDS),
      expiration = TimeUnit.SECONDS.convert(keyRingInfo.expirationDate?.time
          ?: 0, TimeUnit.MILLISECONDS),
      algo = algo,
      passphrase = null,
      errorMsg = null)
}

@Throws(IOException::class)
fun PGPKeyRing.armor(headers: List<Pair<String, String>>? = null): String {
  ByteArrayOutputStream().use { out ->
    ArmoredOutputStream(out).use { armoredOut ->
      if (headers != null) {
        for (header in headers) {
          armoredOut.addHeader(header.first, header.second)
        }
      }
      this.encode(armoredOut)
    }
    return String(out.toByteArray(), StandardCharsets.US_ASCII)
  }
}

val PGPKeyRing.expiration: Instant?
  get() {
    val publicKey = this.publicKey
    return if (publicKey.validSeconds == 0L) {
      null
    } else {
      Instant.ofEpochMilli(publicKey.creationTime.time + publicKey.validSeconds * 1000)
    }
  }
