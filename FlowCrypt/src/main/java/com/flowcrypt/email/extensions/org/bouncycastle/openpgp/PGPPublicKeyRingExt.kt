/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.pgpainless.key.util.SignatureUtils
import java.lang.IllegalStateException

data class PrimaryUserInfo(
    val userId: String,
    val selfCertification: PGPSignature
)

fun PGPPublicKeyRing.getPrimaryUser(): PrimaryUserInfo {
  val publicKey = publicKey
  val u = publicKey.userIDs.asSequence().map {
    Pair(
        it,
        publicKey.getLatestValidSignature(this, PGPSignature.DEFAULT_CERTIFICATION, it)
    )
  }.filter { it.second != null }.toList().minByOrNull {
    val signature = it.second!!
    CompareSignaturesHelper(
        SignatureUtils.isUserIdValid(publicKey, it.first),
        signature.isPrimaryUserId(),
        signature.creationTime.toInstant()
    )
  } ?: throw IllegalStateException("Primary user not found")
  if (SignatureUtils.isUserIdValid(publicKey, u.first)) {
    throw IllegalStateException("Primary user is revoked")
  }
  return PrimaryUserInfo(u.first, u.second!!)
}
