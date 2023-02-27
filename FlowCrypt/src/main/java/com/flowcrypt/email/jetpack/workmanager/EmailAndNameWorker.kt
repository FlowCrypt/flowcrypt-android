/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.RecipientDao
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker

/**
 * This service updates a name of some email entry or creates a new email entry if it not exist.
 *
 * Used a next logic:
 *  *  if email in db:
 *  * if db_row.name is null and bool(name) == true:
 * "save that person's name into the existing DB record"
 *
 *  *  else:
 * "save that email, name pair into DB like so: new RecipientEntity(email, name);"
 *
 * @author Denys Bondarenko
 */
class EmailAndNameWorker(context: Context, params: WorkerParameters) : BaseWorker(context, params) {
  private var recipientDao: RecipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()

  override suspend fun doWork(): Result {
    val emails = inputData.getStringArray(EXTRA_KEY_EMAILS) ?: return Result.failure()
    val names = inputData.getStringArray(EXTRA_KEY_NAMES) ?: return Result.failure()

    if (emails.size != names.size) return Result.failure()

    for (i in emails.indices) {
      val email = emails[i].lowercase()
      val name = names[i]
      val recipientEntity = recipientDao.getRecipientByEmailSuspend(email)
      if (recipientEntity != null) {
        if (recipientEntity.name.isNullOrEmpty()) {
          recipientDao.updateSuspend(recipientEntity.copy(name = name))
        }
      } else {
        recipientDao.insertSuspend(RecipientEntity(email = email, name = name))
      }
    }

    return Result.success()
  }

  companion object {
    const val EXTRA_KEY_EMAILS = BuildConfig.APPLICATION_ID + ".EXTRA_KEY_EMAILS"
    const val EXTRA_KEY_NAMES = BuildConfig.APPLICATION_ID + ".EXTRA_KEY_NAMES"
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".UPDATE_EMAIL_AND_NAME"

    fun enqueue(context: Context, emailAndNamePairs: Collection<Pair<String, String?>>) {
      val emails = emailAndNamePairs.map { it.first }
      val names = emailAndNamePairs.map { it.second }

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.APPEND,
          OneTimeWorkRequestBuilder<EmailAndNameWorker>()
            .addTag(BaseSyncWorker.TAG_SYNC)
            .setInputData(
              workDataOf(
                EXTRA_KEY_EMAILS to emails.toTypedArray(),
                EXTRA_KEY_NAMES to names.toTypedArray()
              )
            )
            .build()
        )
    }
  }
}
