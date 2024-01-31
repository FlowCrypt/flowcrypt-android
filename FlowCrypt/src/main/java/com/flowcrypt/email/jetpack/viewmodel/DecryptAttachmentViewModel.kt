/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class DecryptAttachmentViewModel(val attachmentInfo: AttachmentInfo, application: Application) :
  DecryptDataViewModel(application) {
  private val controlledRunnerForDecrypting = ControlledRunner<Result<ByteArray>>()
  private val decryptAttachmentMutableStateFlow: MutableStateFlow<Result<ByteArray>> =
    MutableStateFlow(Result.loading())
  val decryptAttachmentStateFlow: StateFlow<Result<ByteArray>> =
    decryptAttachmentMutableStateFlow.asStateFlow()

  fun decrypt() {
    viewModelScope.launch {
      val context: Context = getApplication()
      decryptAttachmentMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.decrypting))
      decryptAttachmentMutableStateFlow.value =
        controlledRunnerForDecrypting.cancelPreviousThenRun {
          return@cancelPreviousThenRun decryptInternal(context)
        }
    }
  }

  private suspend fun decryptInternal(context: Context): Result<ByteArray> =
    withContext(Dispatchers.IO) {
      try {
        attachmentInfo.uri?.let { uri ->
          val inputStream =
            context.contentResolver.openInputStream(uri)
              ?: throw IllegalStateException("Uri is not defined")
          return@withContext Result.success(decryptDataIfNeeded(context, inputStream))
        }

        throw IllegalStateException("Uri is not defined")
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        return@withContext Result.exception(e)
      }
    }

  override suspend fun decryptDataIfNeeded(context: Context, inputStream: InputStream): ByteArray =
    withContext(Dispatchers.IO) {
      if (!SecurityUtils.isPossiblyEncryptedData(attachmentInfo.name)) {
        return@withContext inputStream.readBytes()
      }

      decryptAttachmentMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.decrypting))

      return@withContext super.decryptDataIfNeeded(context, inputStream)
    }
}