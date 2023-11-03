/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.model.PgpKeyRingDetails
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
 * @author Denys Bondarenko
 */
open class ParseKeysViewModel(application: Application) : AccountViewModel(application) {
  private val pgpKeyRingDetailsListMutableStateFlow: MutableStateFlow<Result<List<PgpKeyRingDetails>>> =
    MutableStateFlow(Result.none())
  val pgpKeyRingDetailsListStateFlow: StateFlow<Result<List<PgpKeyRingDetails>>> =
    pgpKeyRingDetailsListMutableStateFlow.asStateFlow()
  private val controlledRunnerForParseKeys = ControlledRunner<Result<List<PgpKeyRingDetails>>>()

  fun parseKeys(source: ByteArray?, skipErrors: Boolean = false) {
    source?.let { parseKeys(inputStream = it.inputStream(), skipErrors = skipErrors) }
  }

  fun parseKeys(inputStream: InputStream, skipErrors: Boolean = false) {
    viewModelScope.launch {
      pgpKeyRingDetailsListMutableStateFlow.value = Result.loading()
      pgpKeyRingDetailsListMutableStateFlow.value =
        controlledRunnerForParseKeys.cancelPreviousThenRun {
          return@cancelPreviousThenRun withContext(Dispatchers.IO) {
            try {
              Result.success(
                PgpKey.parseKeys(
                  source = inputStream,
                  skipErrors = skipErrors
                ).pgpKeyDetailsList
              )
            } catch (e: Exception) {
              Result.exception(e)
            }
          }
        }
    }
  }
}
