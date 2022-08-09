/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * We use [CoroutineWorker] to execute pending [Action] in background threads
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:54
 * E-mail: DenBond7@gmail.com
 */
class ActionQueueIntentService(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  override suspend fun doWork(): Result {
    var result = Result.success()
    val attemptsMap = mutableMapOf<Long, Int>()
    val activeAccount =
      roomDatabase.accountDao().getActiveAccountSuspend() ?: return Result.success()

    var actionQueueEntity: ActionQueueEntity?
    while (roomDatabase.actionQueueDao().getActionsByEmailSuspend(activeAccount.email)
        .firstOrNull().also { actionQueueEntity = it } != null
    ) {
      try {
        actionQueueEntity?.toAction()?.run(applicationContext)
        actionQueueEntity?.let { roomDatabase.actionQueueDao().delete(it) }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        result = Result.retry()
      }
    }

    return result
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".ACTIONS_QUEUE"
    fun enqueue(context: Context) {
      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          EmailAndNameWorker.GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<ActionQueueIntentService>()
            .addTag(GROUP_UNIQUE_TAG)
            .build()
        )
    }
  }
}
