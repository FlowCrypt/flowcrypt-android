/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import java.io.ByteArrayInputStream

/**
 * @author Denys Bondarenko
 */
fun <T> Iterable<T>?.toPGPPublicKeyRingCollection(): PGPPublicKeyRingCollection? {
  this ?: return null
  val keysStream = ByteArrayInputStream(this.joinToString(separator = "\n").toByteArray())
  return keysStream.use {
    ArmoredInputStream(it).use { armoredInputStream ->
      PGPainless.readKeyRing().publicKeyRingCollection(armoredInputStream)
    }
  }
}

fun <T> Iterable<T>?.toPGPSecretKeyRingCollection(): PGPSecretKeyRingCollection? {
  this ?: return null
  val keysStream = ByteArrayInputStream(this.joinToString(separator = "\n").toByteArray())
  return keysStream.use {
    ArmoredInputStream(it).use { armoredInputStream ->
      PGPainless.readKeyRing().secretKeyRingCollection(armoredInputStream)
    }
  }
}
