/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.extensions.org.pgpainless.key.longId
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpDecrypt
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.DecryptionException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This class implements [KeysStorage]. Here we collect information about imported private keys
 * for an active account and keep it in the memory.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 *
 * @version 2.0 Updated to use [Passphrase] and [PGPSecretKeyRing]
 */
class KeysStorageImpl private constructor(context: Context) : KeysStorage {
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
  private val passPhraseMap = TreeMap<String, PassPhraseInRAM>(String.CASE_INSENSITIVE_ORDER)

  private val pureActiveAccountLiveData = roomDatabase.accountDao().getActiveAccountLD()

  private val encryptedKeysLiveData = pureActiveAccountLiveData.switchMap {
    roomDatabase.keysDao().getAllKeysByAccountLD(it?.email ?: "")
  }

  private val keysLiveData = encryptedKeysLiveData.switchMap { list ->
    liveData {
      emit(list.map {
        it.copy(
          privateKey = KeyStoreCryptoManager.decryptSuspend(it.privateKeyAsString).toByteArray(),
          storedPassphrase = KeyStoreCryptoManager.decryptSuspend(it.storedPassphrase)
        )
      })
    }
  }

  val secretKeyRingsLiveData = keysLiveData.switchMap {
    liveData {
      val combinedSource =
        it.joinToString(separator = "\n") { keyEntity -> keyEntity.privateKeyAsString }
      val parseKeyResult = PgpKey.parseKeys(combinedSource)
      val keys = parseKeyResult.pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings
        .asSequence().toList()
      emit(keys)
    }
  }

  init {
    keysLiveData.observeForever {
      updatePassphrasesMap(it)
    }
  }

  override fun getRawKeys(): List<KeyEntity> {
    return keysLiveData.value ?: emptyList()
  }

  override fun getPGPSecretKeyRings(): List<PGPSecretKeyRing> {
    return secretKeyRingsLiveData.value ?: emptyList()
  }

  override fun getPgpKeyDetailsList(): List<PgpKeyDetails> {
    val list = mutableListOf<PgpKeyDetails>()
    for (secretKey in secretKeyRingsLiveData.value ?: emptyList()) {
      val pgpKeyDetails = secretKey.toPgpKeyDetails()
      val passphrase = getPassphraseByFingerprint(pgpKeyDetails.fingerprint)
      list.add(pgpKeyDetails.copy(tempPassphrase = passphrase?.chars))
    }
    return list
  }

  override fun getPGPSecretKeyRingByFingerprint(fingerprint: String): PGPSecretKeyRing? {
    return getPGPSecretKeyRings().firstOrNull {
      it.secretKey.publicKey.fingerprint.contentEquals(fingerprint.toByteArray())
    }
  }

  override fun getPGPSecretKeyRingsByFingerprints(fingerprints: Iterable<String>): List<PGPSecretKeyRing> {
    TODO("Not yet implemented")
  }

  override fun getPGPSecretKeyRingsByUserId(user: String): List<PGPSecretKeyRing> {
    TODO("Not yet implemented")
  }

  override fun getPassphraseByFingerprint(fingerprint: String): Passphrase? {
    return passPhraseMap[fingerprint]?.passphrase
  }

  override fun getPassphraseTypeByFingerprint(fingerprint: String): KeyEntity.PassphraseType? {
    return passPhraseMap[fingerprint]?.passphraseType
  }

  override fun getSecretKeyRingProtector(): SecretKeyRingProtector {
    val keyRingProtectionSettings = KeyRingProtectionSettings.secureDefaultSettings()
    val availablePGPSecretKeyRings = getPGPSecretKeyRings()
    return PasswordBasedSecretKeyRingProtector(keyRingProtectionSettings) { keyId ->
      for (pgpSecretKeyRing in availablePGPSecretKeyRings) {
        val keyIDs = pgpSecretKeyRing.secretKeys.iterator().asSequence().map { it.keyID }
        if (keyIDs.contains(keyId)) {
          for (secretKey in pgpSecretKeyRing.secretKeys) {
            val openPgpV4Fingerprint = OpenPgpV4Fingerprint(secretKey)
            val passphrase = getPassphraseByFingerprint(openPgpV4Fingerprint.longId)
              ?: throw DecryptionException(
                decryptionErrorType = PgpDecrypt.DecryptionErrorType.NEED_PASSPHRASE,
                e = PGPException("flowcrypt: need passphrase")
              )
            return@PasswordBasedSecretKeyRingProtector passphrase
          }
        }
      }

      return@PasswordBasedSecretKeyRingProtector null
    }
  }

  override fun updatePassPhrasesCache() {
    for (key in getRawKeys()) {
      if (key.passphraseType == KeyEntity.PassphraseType.RAM) {
        val entry = passPhraseMap[key.fingerprint] ?: continue
        if (entry.passphrase.isEmpty) continue
        val now = Instant.now()
        if (entry.validUntil == now || entry.validUntil.isBefore(now)) {
          passPhraseMap[key.fingerprint] = entry.copy(
            passphrase = Passphrase.emptyPassphrase()
          )
        }
      }
    }
  }

  override fun putPassPhraseToCache(
    fingerprint: String,
    passphrase: Passphrase,
    validUntil: Instant,
    passphraseType: KeyEntity.PassphraseType
  ) {
    passPhraseMap[fingerprint] = PassPhraseInRAM(
      passphrase = passphrase,
      validUntil = validUntil,
      passphraseType = passphraseType
    )
  }

  private fun updatePassphrasesMap(keyEntityList: List<KeyEntity>) {
    val existedIdList = passPhraseMap.keys
    val refreshedIdList = keyEntityList.map { it.fingerprint }
    val removeCandidates = existedIdList - refreshedIdList
    val addCandidates = refreshedIdList - existedIdList
    val updateCandidates = refreshedIdList - addCandidates

    for (id in removeCandidates) {
      passPhraseMap.remove(id)
    }

    for (keyEntity in keyEntityList) {
      val id = keyEntity.fingerprint
      if (id in updateCandidates) {
        if (keyEntity.passphraseType == KeyEntity.PassphraseType.DATABASE) {
          passPhraseMap[id] = PassPhraseInRAM(
            passphrase = keyEntity.passphrase,
            validUntil = Instant.MAX,
            passphraseType = keyEntity.passphraseType
          )
        }
      }

      if (id in addCandidates) {
        when (keyEntity.passphraseType) {
          KeyEntity.PassphraseType.RAM -> {
            passPhraseMap[id] = PassPhraseInRAM(
              passphrase = Passphrase.emptyPassphrase(),
              validUntil = Instant.now(),
              passphraseType = keyEntity.passphraseType
            )
          }

          KeyEntity.PassphraseType.DATABASE -> {
            passPhraseMap[id] = PassPhraseInRAM(
              passphrase = keyEntity.passphrase,
              validUntil = Instant.MAX,
              passphraseType = keyEntity.passphraseType
            )
          }
        }
      }
    }
  }

  interface OnKeysUpdatedListener {
    fun onKeysUpdated()
  }

  private data class PassPhraseInRAM(
    val passphrase: Passphrase,
    val validUntil: Instant,
    val passphraseType: KeyEntity.PassphraseType
  )

  companion object {
    private val MAX_LIFE_TIME_OF_KEYS_IN_RAM = TimeUnit.HOURS.toMillis(4)

    fun calculateLifeTimeForPassphrase(): Instant {
      return Instant.ofEpochMilli(System.currentTimeMillis() + MAX_LIFE_TIME_OF_KEYS_IN_RAM)
    }

    @Volatile
    private var INSTANCE: KeysStorageImpl? = null

    @JvmStatic
    fun getInstance(context: Context): KeysStorageImpl {
      val appContext = context.applicationContext
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: KeysStorageImpl(appContext).also { INSTANCE = it }
      }
    }
  }
}
