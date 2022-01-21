/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import androidx.annotation.Keep
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.time.Instant

@Keep
interface KeysStorage {
  fun getRawKeys(): List<KeyEntity>

  fun getPGPSecretKeyRings(): List<PGPSecretKeyRing>

  fun getPgpKeyDetailsList(): List<PgpKeyDetails>

  fun getPGPSecretKeyRingByFingerprint(fingerprint: String): PGPSecretKeyRing?

  fun getPGPSecretKeyRingsByFingerprints(fingerprints: Collection<String>): List<PGPSecretKeyRing>

  fun getPGPSecretKeyRingsByUserId(user: String): List<PGPSecretKeyRing>

  fun getPassphraseByFingerprint(fingerprint: String): Passphrase?

  fun getPassphraseTypeByFingerprint(fingerprint: String): KeyEntity.PassphraseType?

  fun getSecretKeyRingProtector(): SecretKeyRingProtector

  fun updatePassphrasesCache()

  fun putPassphraseToCache(
    fingerprint: String,
    passphrase: Passphrase,
    validUntil: Instant,
    passphraseType: KeyEntity.PassphraseType
  )

  fun hasEmptyPassphrase(): Boolean

  fun hasPassphrase(passphrase: Passphrase): Boolean

  fun getFingerprintsWithEmptyPassphrase(): List<String>
}
