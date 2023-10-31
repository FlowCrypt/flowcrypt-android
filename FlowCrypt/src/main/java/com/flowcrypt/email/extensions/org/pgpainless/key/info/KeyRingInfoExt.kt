/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import org.bouncycastle.openpgp.PGPPublicKey
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
fun KeyRingInfo.usableForEncryption(): Boolean {
  return !publicKey.hasRevocation()
      && !isExpired()
      && isUsableForEncryption
      && primaryUserId?.isNotEmpty() == true
}

fun KeyRingInfo.isExpired(): Boolean {
  return try {
    primaryKeyExpirationDate?.time?.let { System.currentTimeMillis() > it } ?: false
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

fun KeyRingInfo.getMasterKey(): PGPPublicKey? {
  return publicKeys.firstOrNull { it.isMasterKey }
}
