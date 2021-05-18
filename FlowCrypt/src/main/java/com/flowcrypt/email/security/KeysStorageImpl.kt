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
import com.flowcrypt.email.extensions.org.pgpainless.key.longId
import com.flowcrypt.email.extensions.pgp.toPgpKeyDetails
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.time.Instant

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
  private val passPhraseMap = mutableMapOf<String, PassPhraseInRAM>()

  private val pureActiveAccountLiveData = roomDatabase.accountDao().getActiveAccountLD()

  private val encryptedKeysLiveData = pureActiveAccountLiveData.switchMap {
    roomDatabase.keysDao().getAllKeysByAccountLD(it?.email ?: "")
  }

  private val keysLiveData = encryptedKeysLiveData.switchMap { list ->
    liveData {
      emit(list.map {
        it.copy(
            privateKey = KeyStoreCryptoManager.decryptSuspend(it.privateKeyAsString).toByteArray(),
            storedPassphrase = KeyStoreCryptoManager.decryptSuspend(it.storedPassphrase))
      })
    }
  }

  val secretKeyRingsLiveData = keysLiveData.switchMap {
    liveData {
      val combinedSource = it.joinToString(separator = "\n") { keyEntity -> keyEntity.privateKeyAsString }
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
            return@PasswordBasedSecretKeyRingProtector passphrase
          }
        }
      }

      return@PasswordBasedSecretKeyRingProtector null
    }
  }

  override fun updatePassPhrasesCache() {
    for (key in getRawKeys()) {
      if (key.storedPassphrase == null) {
        val id = key.fingerprint
        if (passPhraseMap.containsKey(id)) {
          val now = Instant.now()
          val entry = passPhraseMap[id] ?: continue
          if (entry.validUntil == now || entry.validUntil.isBefore(now)) {
            passPhraseMap[id] = PassPhraseInRAM(
                passphrase = Passphrase.emptyPassphrase(),
                validUntil = Instant.now(),
                passphraseType = KeyEntity.PassphraseType.RAM)
          }
        }
      }
    }
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
        if (!keyEntity.passphrase.isEmpty) {
          passPhraseMap.remove(id)
        }
      }

      if (id in addCandidates) {
        if (keyEntity.passphrase.isEmpty) {
          passPhraseMap[id] = PassPhraseInRAM(
              passphrase = Passphrase.emptyPassphrase(),
              validUntil = Instant.now(),
              passphraseType = KeyEntity.PassphraseType.RAM)
        } else {
          passPhraseMap[id] = PassPhraseInRAM(
              passphrase = keyEntity.passphrase,
              validUntil = Instant.MAX,
              passphraseType = KeyEntity.PassphraseType.DATABASE)
        }
      }
    }
  }

  interface OnKeysUpdatedListener {
    fun onKeysUpdated()
  }

  private data class PassPhraseInRAM(val passphrase: Passphrase,
                                     val validUntil: Instant,
                                     val passphraseType: KeyEntity.PassphraseType)

  companion object {
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
