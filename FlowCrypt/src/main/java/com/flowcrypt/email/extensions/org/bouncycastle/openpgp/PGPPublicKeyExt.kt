/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.pgpainless.key.util.SignatureUtils
import java.time.Instant

data class CompareSignaturesHelper (
    val revoked: Boolean,
    val isPrimaryUserId: Boolean,
    val creationTime: Instant
) : Comparable<CompareSignaturesHelper> {
  override fun compareTo(other: CompareSignaturesHelper): Int {
    return if (revoked != other.revoked) {
      if (revoked) 1 else -1
    } else if (isPrimaryUserId != other.isPrimaryUserId) {
      if (isPrimaryUserId) 1 else -1
    } else {
      creationTime.compareTo(other.creationTime)
    }
  }
}

fun PGPPublicKey.getLatestValidSignature(
    publicKeyRing: PGPPublicKeyRing,
    signatureType: Int,
    userId: String?
): PGPSignature? {
  val signatures = (if (userId == null) this.signatures else this.getSignaturesForID(userId))
      .asSequence().map { it as PGPSignature }.filter { it.signatureType == signatureType }.toList()
  return SignatureUtils.getLatestValidSignature(this, signatures, publicKeyRing)
}
