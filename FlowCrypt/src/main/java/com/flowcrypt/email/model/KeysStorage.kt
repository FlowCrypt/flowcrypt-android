/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import androidx.annotation.Keep
import com.flowcrypt.email.database.entity.KeyEntity
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase

@Keep
interface KeysStorage {
  fun getRawKeys(): List<KeyEntity>

  fun getPGPSecretKeyRings(): List<PGPSecretKeyRing>

  fun getPGPSecretKeyRingByFingerprint(fingerprint: String): PGPSecretKeyRing?

  fun getPGPSecretKeyRingsByFingerprints(fingerprints: Iterable<String>): List<PGPSecretKeyRing>

  fun getPGPSecretKeyRingsByUserId(user: String): List<PGPSecretKeyRing>

  fun getPassphraseByFingerprint(fingerprint: String): Passphrase?

  fun getSecretKeyRingProtector(): SecretKeyRingProtector

  fun updatePassPhrasesCache()
}


