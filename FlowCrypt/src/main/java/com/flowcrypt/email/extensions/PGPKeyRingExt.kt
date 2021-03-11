/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.flowcrypt.email.api.retrofit.response.model.node.Algo
import com.flowcrypt.email.api.retrofit.response.model.node.KeyId
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.security.pgp.PgpArmorUtils
import org.bouncycastle.openpgp.PGPKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.info.KeyInfo
import org.pgpainless.key.info.KeyRingInfo
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 3/11/21
 *         Time: 10:08 AM
 *         E-mail: DenBond7@gmail.com
 */
fun PGPKeyRing.toNodeKeyDetails(): NodeKeyDetails {
  val keyRingInfo = KeyRingInfo(this)

  val bitStrength = if (keyRingInfo.publicKey.bitStrength != -1) {
    keyRingInfo.publicKey.bitStrength
  } else {
    0
  }

  val ids = publicKeys.iterator().asSequence().toList()
      .map {
        val fingerprint = OpenPgpV4Fingerprint(it)
        KeyId(
            fingerprint = fingerprint.toString(),
            longId = it.keyID.toString(),
            shortId = "",
            keywords = "")
      }

  return NodeKeyDetails(
      isFullyDecrypted = keyRingInfo.isFullyDecrypted,
      isFullyEncrypted = keyRingInfo.isFullyEncrypted(),
      privateKey = if (keyRingInfo.isSecretKey) PgpArmorUtils.toAsciiArmoredString(keyRingInfo.secretKey) else null,
      publicKey = PgpArmorUtils.toAsciiArmoredString(keyRingInfo.publicKey),
      users = keyRingInfo.userIds,
      ids = ids,//fix me, longId, shortId, keywords
      created = TimeUnit.SECONDS.convert(keyRingInfo.creationDate.time, TimeUnit.MILLISECONDS),
      lastModified = TimeUnit.SECONDS.convert(keyRingInfo.lastModified.time, TimeUnit.MILLISECONDS),
      expiration = TimeUnit.SECONDS.convert(keyRingInfo.expirationDate?.time
          ?: 0, TimeUnit.MILLISECONDS),
      algo = Algo(algorithm = keyRingInfo.algorithm.name,
          algorithmId = keyRingInfo.algorithm.algorithmId,
          bits = bitStrength,
          curve = KeyInfo.getCurveName(publicKey)),//fix me, don't show curve
      passphrase = null,
      errorMsg = null)
}
