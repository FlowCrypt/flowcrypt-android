/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
import org.pgpainless.util.Passphrase
import java.time.Instant

/**
 * @author Denis Bondarenko
 *         Date: 5/24/21
 *         Time: 5:33 PM
 *         E-mail: DenBond7@gmail.com
 */
class PgpKeyDetailsViewModel(val fingerprint: String?, application: Application) :
  BaseNodeApiViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())

  private val pgpKeyDetailsLiveDataDirect: LiveData<Result<PgpKeyDetails?>> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        emit(Result.success(list.firstOrNull {
          it.toPgpKeyDetails().fingerprint.equals(
            fingerprint,
            true
          )
        }?.toPgpKeyDetails()))
      }
    }

  private val pgpKeyDetailsLiveDataAfterPassphraseUpdates: LiveData<Result<PgpKeyDetails?>> =
    keysStorage.passphrasesUpdatesLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        val pgpKeyDetailsResult = pgpKeyDetailsLiveDataDirect.value ?: Result.success(
          keysStorage.getPGPSecretKeyRingByFingerprint(fingerprint ?: "")?.toPgpKeyDetails()
        )
        emit(pgpKeyDetailsResult)
      }
    }

  val pgpKeyDetailsLiveData = MediatorLiveData<Result<PgpKeyDetails?>>()

  init {
    pgpKeyDetailsLiveData.addSource(pgpKeyDetailsLiveDataDirect) {
      pgpKeyDetailsLiveData.value = it
    }
    pgpKeyDetailsLiveData.addSource(pgpKeyDetailsLiveDataAfterPassphraseUpdates) {
      pgpKeyDetailsLiveData.value = it
    }
  }

  fun getPgpKeyDetails(): PgpKeyDetails? = pgpKeyDetailsLiveData.value?.data

  fun getPassphrase(): Passphrase? {
    return fingerprint?.let { keysStorage.getPassphraseByFingerprint(it) }
  }

  fun getPassphraseType(): KeyEntity.PassphraseType? {
    return fingerprint?.let { keysStorage.getPassphraseTypeByFingerprint(it) }
  }

  fun forgotPassphrase() {
    fingerprint?.let {
      keysStorage.putPassPhraseToCache(
        fingerprint = it,
        passphrase = Passphrase.emptyPassphrase(),
        validUntil = Instant.now(),
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }
  }

  fun updatePassphrase(passphrase: Passphrase) {
    fingerprint?.let {
      keysStorage.putPassPhraseToCache(
        fingerprint = it,
        passphrase = passphrase,
        validUntil = KeysStorageImpl.calculateLifeTimeForPassphrase(),
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }
  }
}