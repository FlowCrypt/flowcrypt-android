/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForEncryption
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.DecryptionException
import kotlinx.coroutines.flow.Flow
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.key.protection.passphrase_provider.SecretKeyPassphraseProvider
import org.pgpainless.util.Passphrase
import java.time.Instant
import java.util.TreeMap
import java.util.concurrent.TimeUnit

/**
 * This class implements [KeysStorage]. Here we collect information about imported private keys
 * for an active account and keep it in the memory.
 *
 * @author Denys Bondarenko
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
      val combinedSource = it.joinToString(separator = "\n") { keyEntity ->
        keyEntity.privateKeyAsString
      }
      val pgpKeyRingCollection = PgpKey.parseKeysRaw(combinedSource)
      val keys = pgpKeyRingCollection.pgpSecretKeyRingCollection.keyRings.asSequence().toList()
      emit(keys)
    }
  }

  val passphrasesUpdatesLiveData = MutableLiveData(System.currentTimeMillis())

  init {
    keysLiveData.observeForever {
      preparePassphrasesMap(it)
    }
  }

  override fun getRawKeys(): List<KeyEntity> {
    return keysLiveData.value ?: emptyList()
  }

  override fun getPGPSecretKeyRings(): List<PGPSecretKeyRing> {
    /*
    due to https://github.com/bcgit/bc-java/issues/1379 we can't use
    the same PGPSecretKeyRing objects in different threads.
    So we have to make a copy of each PGPSecretKeyRing.
    */

    return secretKeyRingsLiveData.value?.map {
      PGPSecretKeyRing(it.encoded, BcKeyFingerprintCalculator())
    } ?: emptyList()
  }

  override fun getPgpKeyDetailsList(): List<PgpKeyRingDetails> {
    return getPgpKeyDetailsList(getPGPSecretKeyRings())
  }

  @WorkerThread
  @Synchronized
  fun getPgpKeyDetailsList(rings: List<PGPSecretKeyRing>): List<PgpKeyRingDetails> {
    return rings.map {
      val pgpKeyRingDetails = it.toPgpKeyRingDetails()
      val passphrase = getPassphraseByFingerprint(pgpKeyRingDetails.fingerprint)
      pgpKeyRingDetails.copy(tempPassphrase = passphrase?.getChars())
    }
  }

  override fun getPGPSecretKeyRingByFingerprint(fingerprint: String): PGPSecretKeyRing? {
    return getPGPSecretKeyRings().firstOrNull {
      val openPgpV4Fingerprint = OpenPgpV4Fingerprint(it.secretKey)
      openPgpV4Fingerprint.toString().equals(fingerprint, true)
    }
  }

  override fun getPGPSecretKeyRingsByFingerprints(fingerprints: Collection<String>):
      List<PGPSecretKeyRing> {
    val set = fingerprints.map { it.uppercase() }.toSet()
    return getPGPSecretKeyRings().filter {
      OpenPgpV4Fingerprint(it).toString() in set
    }
  }

  override fun getPGPSecretKeyRingsByUserId(user: String): List<PGPSecretKeyRing> {
    val list = mutableListOf<PGPSecretKeyRing>()
    for (secretKey in getPGPSecretKeyRings()) {
      for (userId in secretKey.publicKey.userIDs) {
        try {
          val internetAddresses = userId.asInternetAddresses()
          for (internetAddress in internetAddresses) {
            if (user.equals(internetAddress.address, true)) {
              list.add(secretKey)
              continue
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
    return list
  }

  override fun getPassphraseByFingerprint(fingerprint: String): Passphrase? {
    return passPhraseMap[fingerprint]?.passphrase
  }

  override fun getPassphraseTypeByFingerprint(fingerprint: String): KeyEntity.PassphraseType? {
    return passPhraseMap[fingerprint]?.passphraseType
  }

  override fun getSecretKeyRingProtector(): SecretKeyRingProtector {
    val availablePGPSecretKeyRings = getPGPSecretKeyRings()
    val passphraseProvider = object : SecretKeyPassphraseProvider {
      override fun getPassphraseFor(keyId: Long?): Passphrase? {
        return keyId?.let { doGetPassphrase(keyId, true) }
      }

      override fun hasPassphrase(keyId: Long?): Boolean {
        return keyId != null && doGetPassphrase(keyId, false) != null
      }

      private fun doGetPassphrase(keyId: Long, throwException: Boolean): Passphrase? {
        for (pgpSecretKeyRing in availablePGPSecretKeyRings) {
          val hasMatchingKeyId = pgpSecretKeyRing.secretKeys.iterator().asSequence()
            .map { it.keyID }
            .any { it == keyId }
          if (hasMatchingKeyId) {
            val openPgpV4Fingerprint = OpenPgpV4Fingerprint(pgpSecretKeyRing)
            val fingerprint = openPgpV4Fingerprint.toString()
            val passphrase = getPassphraseByFingerprint(fingerprint)
            if (passphrase == null || passphrase.isEmpty) {
              if (throwException) {
                throw DecryptionException(
                  decryptionErrorType = PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE,
                  e = PGPException("flowcrypt: need passphrase"),
                  fingerprints = listOf(fingerprint)
                )
              } else {
                return null
              }
            }
            return passphrase
          }
        }
        return null
      }
    }
    val keyRingProtectionSettings = KeyRingProtectionSettings.secureDefaultSettings()
    return PasswordBasedSecretKeyRingProtector(keyRingProtectionSettings, passphraseProvider)
  }

  override fun updatePassphrasesCache() {
    val hashCodeBeforeChanges = passPhraseMap.hashCode()

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

    if (hashCodeBeforeChanges != passPhraseMap.hashCode()) {
      passphrasesUpdatesLiveData.postValue(System.currentTimeMillis())
    }
  }

  override fun clearPassphrasesCache() {
    val hashCodeBeforeChanges = passPhraseMap.hashCode()

    for (key in getRawKeys()) {
      if (key.passphraseType == KeyEntity.PassphraseType.RAM) {
        val entry = passPhraseMap[key.fingerprint] ?: continue
        passPhraseMap[key.fingerprint] = entry.copy(
          passphrase = Passphrase.emptyPassphrase(),
          validUntil = Instant.now()
        )
      }
    }

    if (hashCodeBeforeChanges != passPhraseMap.hashCode()) {
      passphrasesUpdatesLiveData.postValue(System.currentTimeMillis())
    }
  }

  override fun putPassphraseToCache(
    fingerprint: String,
    passphrase: Passphrase,
    validUntil: Instant,
    passphraseType: KeyEntity.PassphraseType
  ) {
    val hashCodeBeforeChanges = passPhraseMap.hashCode()

    passPhraseMap[fingerprint] = PassPhraseInRAM(
      passphrase = passphrase,
      validUntil = validUntil,
      passphraseType = passphraseType
    )

    if (hashCodeBeforeChanges != passPhraseMap.hashCode()) {
      passphrasesUpdatesLiveData.postValue(System.currentTimeMillis())
    }
  }

  override fun hasEmptyPassphrase(vararg types: KeyEntity.PassphraseType): Boolean {
    return passPhraseMap.values
      .any { entry -> entry.passphraseType in types && entry.passphrase.isEmpty }
  }

  override fun hasNonEmptyPassphrase(vararg types: KeyEntity.PassphraseType): Boolean {
    return passPhraseMap.values
      .any { entry -> entry.passphraseType in types && !entry.passphrase.isEmpty }
  }

  override fun hasPassphrase(passphrase: Passphrase): Boolean {
    return passPhraseMap.values.any { it.passphrase == passphrase }
  }

  override fun getFingerprintsWithEmptyPassphrase(): List<String> {
    return passPhraseMap.filter { it.value.passphrase.isEmpty }.map { it.key }
  }

  override fun getFirstUsableForEncryptionPGPSecretKeyRing(user: String): PGPSecretKeyRing? {
    return getPGPSecretKeyRingsByUserId(user).firstOrNull { KeyRingInfo(it).usableForEncryption }
  }

  override fun getPassPhrasesUpdatesFlow(): Flow<Long> = passphrasesUpdatesLiveData.asFlow()

  override fun getActiveAccount(): AccountEntity? = pureActiveAccountLiveData.value

  private fun preparePassphrasesMap(keyEntityList: List<KeyEntity>) {
    val existedIdList = passPhraseMap.keys
    val refreshedIdList = keyEntityList.map { it.fingerprint }
    val removeCandidates = existedIdList - refreshedIdList.toSet()
    val addCandidates = refreshedIdList - existedIdList
    val updateCandidates = refreshedIdList - addCandidates.toSet()

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

  fun calculateLifeTimeForPassphrase(): Instant {
    val timeInSeconds =
      pureActiveAccountLiveData.value?.clientConfiguration?.inMemoryPassPhraseSessionLengthNormalized?.toLong()
        ?: DEFAULT_MAX_LIFE_TIME_OF_KEYS_IN_RAM_IN_SECONDS

    val timeInMilliseconds = TimeUnit.SECONDS.toMillis(timeInSeconds)
    return Instant.ofEpochMilli(System.currentTimeMillis() + timeInMilliseconds)
  }

  private data class PassPhraseInRAM(
    val passphrase: Passphrase,
    val validUntil: Instant,
    val passphraseType: KeyEntity.PassphraseType
  )

  companion object {
    private val DEFAULT_MAX_LIFE_TIME_OF_KEYS_IN_RAM_IN_SECONDS = TimeUnit.HOURS.toSeconds(4)

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
