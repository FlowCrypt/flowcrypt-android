/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import java.io.File

/**
 * @author Denys Bondarenko
 */
class PrepareDownloadedAttachmentsForForwardingViewModel(
  private val attachments: Array<AttachmentInfo>,
  application: Application
) : AccountViewModel(application) {
  private val controlledRunnerForPreparingAttachmentsForForwarding =
    ControlledRunner<Result<List<AttachmentInfo>>>()
  private val preparingAttachmentsForForwardingMutableStateFlow: MutableStateFlow<Result<List<AttachmentInfo>>> =
    MutableStateFlow(Result.none())
  val preparingAttachmentsForForwardingStateFlow: StateFlow<Result<List<AttachmentInfo>>> =
    preparingAttachmentsForForwardingMutableStateFlow.asStateFlow()

  fun prepare() {
    viewModelScope.launch {
      val context: Context = getApplication()
      preparingAttachmentsForForwardingMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.processing_please_wait))
      preparingAttachmentsForForwardingMutableStateFlow.value =
        controlledRunnerForPreparingAttachmentsForForwarding.cancelPreviousThenRun {
          return@cancelPreviousThenRun prepareInternal(context)
        }
    }
  }

  private suspend fun prepareInternal(context: Context): Result<List<AttachmentInfo>> =
    withContext(Dispatchers.IO) {
      val list = mutableListOf<AttachmentInfo>()
      for (attachmentInfo in attachments) {
        try {
          if (attachmentInfo.rawData == null) {
            throw NullPointerException("attachmentInfo.rawData == null")
          }

          val isAttachmentEncrypted =
            FilenameUtils.getExtension(attachmentInfo.name).equals(Constants.PGP_FILE_EXT, true)
          val cacheDirectory = CacheManager.getCurrentMsgTempDirectory(context)
          val originalAttName = attachmentInfo.getSafeName()
          var tempFile = File(
            cacheDirectory, originalAttName +
                if (isAttachmentEncrypted) {
                  ""
                } else {
                  "." + Constants.PGP_FILE_EXT
                }
          )


          if (tempFile.exists()) {
            tempFile = FileAndDirectoryUtils.createFileWithIncreasedIndex(
              cacheDirectory, tempFile.name
            )
          }

          if (isAttachmentEncrypted) {
            tempFile.outputStream().use { outputStream ->
              attachmentInfo.rawData.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
              }
            }
          } else {
            val account = getActiveAccountSuspend()
              ?: throw NullPointerException("active account is null")
            val pubKeys = SecurityUtils.getSenderPublicKeys(context, account.email)

            PgpEncryptAndOrSign.encryptAndOrSign(
              srcInputStream = attachmentInfo.rawData.inputStream(),
              destOutputStream = tempFile.outputStream(),
              pubKeys = pubKeys,
              fileName = originalAttName,
            )
          }

          val uri = FileProvider.getUriForFile(
            context, Constants.FILE_PROVIDER_AUTHORITY, tempFile
          )
          list.add(
            attachmentInfo.copy(
              rawData = null,
              type = Constants.MIME_TYPE_BINARY_DATA,
              uri = uri,
              name = tempFile.name
            )
          )
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
        }
      }

      return@withContext Result.success(list)
    }
}