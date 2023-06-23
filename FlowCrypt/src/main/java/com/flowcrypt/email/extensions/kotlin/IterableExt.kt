/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless
import java.io.ByteArrayInputStream

/**
 * @author Denys Bondarenko
 */
fun <T> Iterable<T>?.toPGPPublicKeyRingCollection(): PGPPublicKeyRingCollection? {
  this ?: return null
  val pubKeysStream = ByteArrayInputStream(this.joinToString(separator = "\n").toByteArray())
  return pubKeysStream.use {
    ArmoredInputStream(it).use { armoredInputStream ->
      PGPainless.readKeyRing().publicKeyRingCollection(armoredInputStream)
    }
  }
}
