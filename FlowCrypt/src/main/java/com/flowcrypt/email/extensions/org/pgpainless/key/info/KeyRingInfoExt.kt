/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import org.pgpainless.algorithm.EncryptionPurpose
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denis Bondarenko
 *         Date: 12/2/21
 *         Time: 1:26 PM
 *         E-mail: DenBond7@gmail.com
 */
fun KeyRingInfo.usableForEncryption(): Boolean {
  val isExpired = try {
    System.currentTimeMillis() > primaryKeyExpirationDate?.time ?: 0
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }

  return !publicKey.hasRevocation()
      && !isExpired
      && getEncryptionSubkeys(EncryptionPurpose.STORAGE_AND_COMMUNICATIONS).isNotEmpty()
      && primaryUserId?.isNotEmpty() == true
}
