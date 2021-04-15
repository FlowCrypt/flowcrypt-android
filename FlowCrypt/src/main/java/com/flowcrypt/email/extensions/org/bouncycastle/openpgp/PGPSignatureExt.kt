/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.openpgp.PGPSignature
import java.time.Instant

fun PGPSignature.isValidDecryptionKeyUnchecked(): Boolean {
  val keyFlags = hashedSubPackets.keyFlags
  return keyFlags == 0
      || (keyFlags and KeyFlags.ENCRYPT_COMMS) != 0
      || (keyFlags and KeyFlags.ENCRYPT_STORAGE) != 0
}

fun PGPSignature.isPrimaryUserId(): Boolean {
  return if (hasSubpackets()) hashedSubPackets.isPrimaryUserID else false
}

fun PGPSignature.isExpired(date: Instant = Instant.now()): Boolean {
  if (!hasSubpackets()) return false
  val expTime = hashedSubPackets.signatureExpirationTime
  return expTime != 0L && !creationTime.toInstant().plusSeconds(expTime).isAfter(date)
}
