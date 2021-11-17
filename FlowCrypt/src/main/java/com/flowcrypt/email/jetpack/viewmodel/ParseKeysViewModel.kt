/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * This [ViewModel] implementation can be used to fetch details about the given keys.
 *
 * @author Denis Bondarenko
 *         Date: 9/21/19
 *         Time: 2:24 PM
 *         E-mail: DenBond7@gmail.com
 */
open class ParseKeysViewModel(application: Application) : AccountViewModel(application) {
  private val pgpKeyDetailsListMutableStateFlow: MutableStateFlow<Result<List<PgpKeyDetails>>> =
    MutableStateFlow(Result.none())
  val pgpKeyDetailsListStateFlow: StateFlow<Result<List<PgpKeyDetails>>> =
    pgpKeyDetailsListMutableStateFlow.asStateFlow()
  private val controlledRunnerForParseKeys = ControlledRunner<Result<List<PgpKeyDetails>>>()

  fun parseKeys(source: ByteArray?) {
    source?.let { parseKeys(it.inputStream()) }
  }

  fun parseKeys(inputStream: InputStream) {
    viewModelScope.launch {
      pgpKeyDetailsListMutableStateFlow.value = Result.loading()
      pgpKeyDetailsListMutableStateFlow.value = controlledRunnerForParseKeys.cancelPreviousThenRun {
        return@cancelPreviousThenRun withContext(Dispatchers.IO) {
          try {
            Result.success(PgpKey.parseKeys(inputStream).pgpKeyDetailsList)
          } catch (e: Exception) {
            Result.exception(e)
          }
        }
      }
    }
  }
}
