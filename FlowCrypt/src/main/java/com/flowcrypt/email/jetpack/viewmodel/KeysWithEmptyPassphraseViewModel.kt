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
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails

/**
 * @author Denys Bondarenko
 */
class KeysWithEmptyPassphraseViewModel(application: Application) : AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())

  val keysWithEmptyPassphrasesLiveData: MediatorLiveData<Result<List<PgpKeyRingDetails>>> =
    MediatorLiveData()

  private val pgpKeyRingDetailsLiveData: LiveData<Result<List<PgpKeyRingDetails>>> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        emit(
          try {
            val account = getActiveAccountSuspend()
            Result.success(
              list
                .map {
                  it.toPgpKeyRingDetails(
                    account?.clientConfiguration?.shouldHideArmorMeta() ?: false
                  )
                }
              .filter { keysStorage.getPassphraseByFingerprint(it.fingerprint)?.isEmpty == true }
              .map {
                it.copy(passphraseType = keysStorage.getPassphraseTypeByFingerprint(it.fingerprint))
              })
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  private val afterPassphraseUpdatedKeyDetailsLiveData: LiveData<Result<List<PgpKeyRingDetails>>> =
    keysStorage.passphrasesUpdatesLiveData.switchMap {
      liveData {
        emit(Result.loading())
        emit(
          try {
            Result.success(keysStorage.getPgpKeyDetailsList()
              .filter { keysStorage.getPassphraseByFingerprint(it.fingerprint)?.isEmpty == true }
              .map {
                it.copy(passphraseType = keysStorage.getPassphraseTypeByFingerprint(it.fingerprint))
              })
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  init {
    keysWithEmptyPassphrasesLiveData.addSource(pgpKeyRingDetailsLiveData) {
      keysWithEmptyPassphrasesLiveData.value = it
    }
    keysWithEmptyPassphrasesLiveData.addSource(afterPassphraseUpdatedKeyDetailsLiveData) {
      keysWithEmptyPassphrasesLiveData.value = it
    }
  }
}
