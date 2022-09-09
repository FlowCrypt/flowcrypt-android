/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.AccountEntity
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

class UploadDraftsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    uploadDrafts(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    uploadDrafts(accountEntity)
  }

  private suspend fun uploadDrafts(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      uploadDraftsInternal(account) { mimeMessage ->
        //to update IMAP draft we have to delete the old one and add a new one
        /*val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
        val folderDrafts = foldersManager.folderDrafts ?: return@uploadDraftsInternal

        store.getFolder(folderDrafts.fullName).use { folder ->
          val draftsFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          draftsFolder.appendMessages(arrayOf<Message>(mimeMessage.apply {
            setFlag(Flags.Flag.DRAFT, true)
            setFlag(Flags.Flag.SEEN, true)
          }))
        }*/
      }
    }

  private suspend fun uploadDrafts(account: AccountEntity) = withContext(Dispatchers.IO) {
    uploadDraftsInternal(account) { mimeMessage ->
      executeGMailAPICall(applicationContext) {
        GmailApiHelper.uploadDraft(
          context = applicationContext,
          account = account,
          mimeMessage = mimeMessage,
          draftId = null
        )
      }
    }
  }

  private suspend fun uploadDraftsInternal(
    account: AccountEntity,
    action: suspend (mimeMessage: MimeMessage) -> Unit
  ) = withContext(Dispatchers.IO) {
    val mimeMessage = MimeMessage(Session.getInstance(Properties())).apply {
      subject = "test drafts from Android:" + System.currentTimeMillis()
      setContent(MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply {
          setText("some text + time:" + System.currentTimeMillis())
        })
      })
    }
    action.invoke(mimeMessage)
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".UPLOAD_DRAFTS"

    const val PREFIX_DELETE = "delete"
    const val PREFIX_UPDATE = "update"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.APPEND,
          OneTimeWorkRequestBuilder<UploadDraftsWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
