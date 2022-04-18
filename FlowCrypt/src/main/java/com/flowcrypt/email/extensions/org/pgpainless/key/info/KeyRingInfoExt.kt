/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denis Bondarenko
 *         Date: 12/2/21
 *         Time: 1:26 PM
 *         E-mail: DenBond7@gmail.com
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
