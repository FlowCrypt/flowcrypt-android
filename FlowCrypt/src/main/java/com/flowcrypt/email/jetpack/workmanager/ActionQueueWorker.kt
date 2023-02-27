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
import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * We use [CoroutineWorker] to execute pending [Action] in background threads
 *
 * @author Denys Bondarenko
 */
class ActionQueueWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  override suspend fun doWork(): Result {
    var result = Result.success()
    val attemptsMap = mutableMapOf<Long, Int>()
    val activeAccount =
      roomDatabase.accountDao().getActiveAccountSuspend() ?: return Result.success()

    while (true) {
      val notActionableActionsIds = attemptsMap.filter { it.value >= MAX_ATTEMPT_PER_ACTION }
      val existingActions =
        roomDatabase.actionQueueDao().getActionsByEmailSuspend(activeAccount.email)
      val actionableActions = existingActions.filter { it.id !in notActionableActionsIds }
      if (actionableActions.isNotEmpty()) {
        for (actionQueueEntity in actionableActions) {
          try {
            actionQueueEntity.toAction()?.run(applicationContext)
            roomDatabase.actionQueueDao().delete(actionQueueEntity)
          } catch (e: Exception) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
            result = Result.retry()
            actionQueueEntity.id?.let {
              val existingValue = attemptsMap[actionQueueEntity.id] ?: 0
              attemptsMap[actionQueueEntity.id] = existingValue + 1
            }
          }
        }
      } else break
    }

    return result
  }

  companion object {
    private const val MAX_ATTEMPT_PER_ACTION = 3
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".ACTIONS_QUEUE"
    fun enqueue(context: Context) {
      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          EmailAndNameWorker.GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<ActionQueueWorker>()
            .addTag(GROUP_UNIQUE_TAG)
            .build()
        )
    }
  }
}
