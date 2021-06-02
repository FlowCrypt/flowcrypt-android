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
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails

/**
 * @author Denis Bondarenko
 *         Date: 6/1/21
 *         Time: 9:59 AM
 *         E-mail: DenBond7@gmail.com
 */
class KeysWithEmptyPassphraseViewModel(application: Application) : AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())

  val keysWithEmptyPassphrasesLiveData: MediatorLiveData<Result<List<PgpKeyDetails>>> =
    MediatorLiveData()

  private val pgpKeyDetailsLiveData: LiveData<Result<List<PgpKeyDetails>>> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        emit(Result.success(list
          .map { it.toPgpKeyDetails() }
          .filter { keysStorage.getPassphraseByFingerprint(it.fingerprint)?.isEmpty == true })
        )
      }
    }

  private val afterPassphraseUpdatedKeyDetailsLiveData: LiveData<Result<List<PgpKeyDetails>>> =
    keysStorage.passphrasesUpdatesLiveData.switchMap {
      liveData {
        emit(Result.loading())
        emit(
          Result.success(keysStorage.getPgpKeyDetailsList()
            .filter { keysStorage.getPassphraseByFingerprint(it.fingerprint)?.isEmpty == true })
        )
      }
    }

  init {
    keysWithEmptyPassphrasesLiveData.addSource(pgpKeyDetailsLiveData) {
      keysWithEmptyPassphrasesLiveData.value = it
    }
    keysWithEmptyPassphrasesLiveData.addSource(afterPassphraseUpdatedKeyDetailsLiveData) {
      keysWithEmptyPassphrasesLiveData.value = it
    }
  }
}
