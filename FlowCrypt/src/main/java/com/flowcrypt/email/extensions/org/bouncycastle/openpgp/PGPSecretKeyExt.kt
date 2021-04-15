/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless

fun PGPSecretKey.isDecrypted() : Boolean {
  // follow openpgp.js
  return this.s2KUsage == 0
}

fun PGPSecretKey.isEncrypted() : Boolean {
  // follow openpgp.js
  return this.s2KUsage != 0
}

fun PGPSecretKey.decrypt(passphrase: CharArray): PGPSecretKey {
  return PGPSecretKeyRing(listOf(this)).decrypt(passphrase).first()
}
