/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPublicKeyRing
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.util.Passphrase
import java.time.Instant

/**
 * @author Denys Bondarenko
 */
class PrivateKeyDetailsViewModel(val fingerprint: String?, application: Application) :
  AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())

  private val pgpSecretKeyRingLiveData: LiveData<PGPSecretKeyRing?> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(
          try {
            list.firstOrNull {
              val openPgpV4Fingerprint = OpenPgpV4Fingerprint(it)
              openPgpV4Fingerprint.toString().equals(fingerprint, true)
            }
          } catch (e: Exception) {
            null
          }
        )
      }
    }

  val publicKeyKeyRingInfoLiveData: LiveData<KeyRingInfo?> =
    pgpSecretKeyRingLiveData.switchMap { pgpSecretKeyRing ->
      liveData {
        emit(
          try {
            pgpSecretKeyRing?.let { KeyRingInfo(it.toPublicKeyRing()) }
          } catch (e: Exception) {
            null
          }
        )
      }
    }

  private val pgpKeyRingDetailsLiveDataDirect: LiveData<Result<PgpKeyRingDetails?>> =
    pgpSecretKeyRingLiveData.switchMap { keyRingInfo ->
      liveData {
        emit(Result.loading())
        emit(
          try {
            val account = getActiveAccountSuspend()
            Result.success(
              keyRingInfo?.toPgpKeyRingDetails(
                account?.clientConfiguration?.shouldHideArmorMeta() ?: false
              )
            )
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  private val pgpKeyRingDetailsLiveDataAfterPassphraseUpdates: LiveData<Result<PgpKeyRingDetails?>> =
    keysStorage.passphrasesUpdatesLiveData.switchMap {
      liveData {
        emit(Result.loading())
        emit(
          try {
            val account = getActiveAccountSuspend()
            Result.success(
              keysStorage.getPGPSecretKeyRingByFingerprint(fingerprint ?: "")?.toPgpKeyRingDetails(
                account?.clientConfiguration?.shouldHideArmorMeta() ?: false
              )
            )
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  val pgpKeyRingDetailsLiveData = MediatorLiveData<Result<PgpKeyRingDetails?>>()

  init {
    pgpKeyRingDetailsLiveData.addSource(pgpKeyRingDetailsLiveDataDirect) {
      pgpKeyRingDetailsLiveData.value = it
    }
    pgpKeyRingDetailsLiveData.addSource(pgpKeyRingDetailsLiveDataAfterPassphraseUpdates) {
      pgpKeyRingDetailsLiveData.value = it
    }
  }

  fun getPgpKeyDetails(): PgpKeyRingDetails? = pgpKeyRingDetailsLiveData.value?.data

  fun getPassphrase(): Passphrase? {
    return fingerprint?.let { keysStorage.getPassphraseByFingerprint(it) }
  }

  fun getPassphraseType(): KeyEntity.PassphraseType? {
    return fingerprint?.let { keysStorage.getPassphraseTypeByFingerprint(it) }
  }

  fun forgetPassphrase() {
    fingerprint?.let {
      keysStorage.putPassphraseToCache(
        fingerprint = it,
        passphrase = Passphrase.emptyPassphrase(),
        validUntil = Instant.now(),
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }
    val context: Context = getApplication()
    context.toast(context.getString(R.string.passphrase_purged_from_memory))
  }

  fun updatePassphrase(passphrase: Passphrase) {
    fingerprint?.let {
      keysStorage.putPassphraseToCache(
        fingerprint = it,
        passphrase = passphrase,
        validUntil = keysStorage.calculateLifeTimeForPassphrase(),
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }
  }
}
