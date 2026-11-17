/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.pgpainless.PGPainless

class IterableExtTest {

  @Test
  fun testToPGPPublicKeyRingCollectionFromPublicKeys() {
    val firstKey = genKey("first@flowcrypt.test")
    val secondKey = genKey("second@flowcrypt.test")

    val collection = listOf(
      firstKey.toCertificate().toAsciiArmoredString(),
      secondKey.toCertificate().toAsciiArmoredString()
    ).toPGPPublicKeyRingCollection()

    assertNotNull(collection)
    assertEquals(2, collection?.size())
    assertNotNull(collection?.getPublicKeyRing(firstKey.keyIdentifier.keyId))
    assertNotNull(collection?.getPublicKeyRing(secondKey.keyIdentifier.keyId))
  }

  @Test
  fun testToPGPPublicKeyRingCollectionFromPrivateKey() {
    val key = genKey("private@flowcrypt.test")

    val collection = listOf(key.toAsciiArmoredString()).toPGPPublicKeyRingCollection()

    assertNotNull(collection)
    assertEquals(1, collection?.size())
    assertNotNull(collection?.getPublicKeyRing(key.keyIdentifier.keyId))
  }

  @Test
  fun testToPGPSecretKeyRingCollectionFromPrivateKeys() {
    val firstKey = genKey("first@flowcrypt.test")
    val secondKey = genKey("second@flowcrypt.test")

    val collection = listOf(
      firstKey.toAsciiArmoredString(),
      secondKey.toAsciiArmoredString()
    ).toPGPSecretKeyRingCollection()

    assertNotNull(collection)
    assertEquals(2, collection?.size())
  }

  private fun genKey(userId: String) =
    PGPainless.getInstance().generateKey().simpleEcKeyRing(userId)
}
