/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.pgp

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.util.KeyRingUtils

fun PGPSecretKeyRing.toPublicKeyRing(): PGPPublicKeyRing {
  return KeyRingUtils.publicKeyRingFrom(this)
}
