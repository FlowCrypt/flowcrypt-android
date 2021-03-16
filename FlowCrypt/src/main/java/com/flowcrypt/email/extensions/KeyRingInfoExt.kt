/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import org.pgpainless.algorithm.SymmetricKeyAlgorithm
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denis Bondarenko
 *         Date: 3/11/21
 *         Time: 10:33 AM
 *         E-mail: DenBond7@gmail.com
 */

/**
 * Return true when every secret key on the key ring is encrypted.
 * If there is at least one unencrypted secret key on the ring, return false.
 * If the ring is a [PGPPublicKeyRing], return false.
 *
 * @return true if all secret keys are encrypted.
 */
fun KeyRingInfo.isFullyEncrypted(): Boolean {
  if (isSecretKey) {
    for (secretKey in secretKeys) {
      if (secretKey.keyEncryptionAlgorithm == SymmetricKeyAlgorithm.NULL.algorithmId) {
        return false
      }
    }

    return true
  } else return false
}