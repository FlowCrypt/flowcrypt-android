/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: ivan
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.util.KeyRingUtils

fun PGPSecretKeyRing.toPublicKeyRing(): PGPPublicKeyRing {
  return KeyRingUtils.publicKeyRingFrom(this)
}
