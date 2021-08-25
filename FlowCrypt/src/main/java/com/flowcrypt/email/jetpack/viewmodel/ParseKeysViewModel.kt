/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey

/**
 * This [ViewModel] implementation can be used to fetch details about the given keys.
 *
 * @author Denis Bondarenko
 *         Date: 9/21/19
 *         Time: 2:24 PM
 *         E-mail: DenBond7@gmail.com
 */
class ParseKeysViewModel(application: Application) : AccountViewModel(application) {
  private val keysSourceLiveData = MutableLiveData<ByteArray>()
  val parseKeysLiveData: LiveData<Result<List<PgpKeyDetails>>> =
    Transformations.switchMap(keysSourceLiveData) { source ->
      liveData {
        emit(Result.loading())
        emit(
          try {
            Result.success(PgpKey.parseKeys(source).pgpKeyDetailsList)
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  fun fetchKeys(source: ByteArray?) {
    keysSourceLiveData.value = source ?: byteArrayOf()
  }
}
