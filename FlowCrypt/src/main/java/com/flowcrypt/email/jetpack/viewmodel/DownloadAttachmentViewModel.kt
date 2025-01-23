/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ExceptionUtil
import jakarta.mail.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class DownloadAttachmentViewModel(val attachmentInfo: AttachmentInfo, application: Application) :
  DecryptDataViewModel(application) {
  private val controlledRunnerForDownloading = ControlledRunner<Result<ByteArray>>()
  private val downloadAttachmentMutableStateFlow: MutableStateFlow<Result<ByteArray>> =
    MutableStateFlow(Result.loading())
  val downloadAttachmentStateFlow: StateFlow<Result<ByteArray>> =
    downloadAttachmentMutableStateFlow.asStateFlow()

  fun download() {
    viewModelScope.launch {
      val context: Context = getApplication()
      downloadAttachmentMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.downloading))
      downloadAttachmentMutableStateFlow.value =
        controlledRunnerForDownloading.cancelPreviousThenRun {
          return@cancelPreviousThenRun downloadInternal(context)
        }
    }
  }

  private suspend fun downloadInternal(context: Context): Result<ByteArray> =
    withContext(Dispatchers.IO) {
      try {
        attachmentInfo.uri?.let { uri ->
          val inputStream = context.contentResolver.openInputStream(uri)
          if (inputStream != null) {
            return@withContext Result.success(
              decryptDataIfNeeded(context, inputStream)
            )
          }
        }

        val email = requireNotNull(attachmentInfo.email)
        val account = requireNotNull(
          roomDatabase.accountDao().getAccount(email)?.withDecryptedInfo()
        )

        if (account.useAPI) {
          when (account.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              return@withContext Result.success(downloadAttachmentViaGmailAPI(context, account))
            }

            else -> throw IllegalStateException("Unsupported provider")
          }
        } else {
          return@withContext Result.success(downloadAttachmentViaIMAP(context, account, email))
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        return@withContext Result.exception(e)
      }
    }

  private suspend fun downloadAttachmentViaGmailAPI(
    context: Context,
    account: AccountEntity
  ): ByteArray = withContext(Dispatchers.IO) {
    val msg = GmailApiHelper.loadMsgInfo(
      context = context,
      accountEntity = account,
      msgId = attachmentInfo.uid.toHex(),
      format = GmailApiHelper.RESPONSE_FORMAT_FULL
    )
    val attPart = GmailApiHelper.getAttPartByPath(msg.payload, neededPath = attachmentInfo.path)
      ?: throw IllegalStateException(context.getString(R.string.attachment_not_found))

    GmailApiHelper.getAttInputStream(
      context = context,
      accountEntity = account,
      msgId = attachmentInfo.uid.toHex(),
      attId = attPart.body.attachmentId
    ).use { inputStream ->
      return@withContext decryptDataIfNeeded(
        context = context,
        inputStream = downloadFile(inputStream).inputStream()
      )
    }
  }

  private suspend fun downloadAttachmentViaIMAP(
    context: Context,
    account: AccountEntity,
    email: String
  ): ByteArray = withContext(Dispatchers.IO) {
    val session = OpenStoreHelper.getAttsSess(context, account)
    OpenStoreHelper.openStore(context, account, session).use { store ->
      val label = roomDatabase.labelDao()
        .getLabel(email, account.accountType, requireNotNull(attachmentInfo.folder))
        ?: if (roomDatabase.accountDao().getAccount(email) == null) {
          return@withContext byteArrayOf()
        } else throw IllegalArgumentException("Folder \"" + attachmentInfo.folder + "\" not found in the local cache")

      store.getFolder(label.name).use { folder ->
        val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
        val msg = remoteFolder.getMessageByUID(attachmentInfo.uid)
          ?: throw IllegalStateException(context.getString(R.string.no_message_with_this_attachment))

        ImapProtocolUtil.getAttPartByPath(
          part = msg,
          neededPath = attachmentInfo.path
        )?.inputStream?.let { inputStream ->
          return@withContext decryptDataIfNeeded(
            context = context,
            inputStream = downloadFile(inputStream).inputStream()
          )
        } ?: throw IllegalStateException(context.getString(R.string.attachment_not_found))
      }
    }
  }

  override suspend fun decryptDataIfNeeded(context: Context, inputStream: InputStream): ByteArray =
    withContext(Dispatchers.IO) {
      if (!SecurityUtils.isPossiblyEncryptedData(attachmentInfo.name)) {
        return@withContext inputStream.readBytes()
      }

      downloadAttachmentMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.decrypting))

      return@withContext super.decryptDataIfNeeded(context, inputStream)
    }

  private suspend fun downloadFile(inputStream: InputStream): ByteArray =
    withContext(Dispatchers.IO) {
      val outputStream = ByteArrayOutputStream()
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      var count = 0.0
      val size = attachmentInfo.encodedSize.toDouble()
      var numberOfReadBytes: Int
      var lastPercentage = 0
      var currentPercentage = 0
      var elapsedTime: Long
      val startTime: Long = System.currentTimeMillis()
      var lastUpdateTime = startTime
      updateProgress(currentPercentage, 0)
      while (true) {
        numberOfReadBytes = inputStream.read(buffer)

        if (IOUtils.EOF == numberOfReadBytes) {
          break
        }

        if (isActive) {
          outputStream.write(buffer, 0, numberOfReadBytes)
          count += numberOfReadBytes.toDouble()
          currentPercentage = (count / size * 100f).toInt()
          val isUpdateNeeded =
            System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
          if (currentPercentage - lastPercentage >= 1 && isUpdateNeeded) {
            lastPercentage = currentPercentage
            lastUpdateTime = System.currentTimeMillis()
            elapsedTime = lastUpdateTime - startTime
            val predictLoadingTime = (elapsedTime * size / count).toLong()
            updateProgress(currentPercentage, predictLoadingTime - elapsedTime)
          }
        } else {
          break
        }
      }

      updateProgress(100, 0)
      return@withContext outputStream.toByteArray()
    }

  private fun updateProgress(currentPercentage: Int, timeLeft: Long) {
    downloadAttachmentMutableStateFlow.value = Result.loading(
      progressMsg = timeLeft.toString(),
      progress = currentPercentage.toDouble()
    )
  }

  companion object {
    private const val MIN_UPDATE_PROGRESS_INTERVAL = 500
    private const val DEFAULT_BUFFER_SIZE = 1024 * 16
  }
}
