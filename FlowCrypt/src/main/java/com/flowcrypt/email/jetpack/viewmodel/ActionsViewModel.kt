/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.util.LongSparseArray
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.service.actionqueue.ActionQueueIntentService
import com.flowcrypt.email.service.actionqueue.ActionResultReceiver
import com.flowcrypt.email.service.actionqueue.actions.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * This class is a manager which checks and runs [Action]
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 17:26
 * E-mail: DenBond7@gmail.com
 */

class ActionsViewModel(application: Application) : RoomBasicViewModel(application), ActionResultReceiver.ResultReceiverCallBack {
  private val runningActions: LongSparseArray<Action> = LongSparseArray()
  private val completedActionsSet: MutableSet<Long> = HashSet()

  override fun onSuccess(action: Action?) {
    action?.let {
      completedActionsSet.add(it.id)
      runningActions.delete(it.id)

      viewModelScope.launch {
        withContext(Dispatchers.IO) {
          roomDatabase.actionQueueDao().deleteByIdSuspend(it.id)
        }
      }
    }
  }

  override fun onError(exception: Exception, action: Action?) {
    action?.id?.let { runningActions.delete(it) }
  }

  /**
   * Check and add actions to the worker queue.
   *
   * @param account The [AccountDao] which has some actions.
   */
  fun checkAndAddActionsToQueue(account: AccountDao) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        val actions = roomDatabase.actionQueueDao().getActionsByEmailSuspend(account.email)
            .map { it.toAction() }
        val candidates = ArrayList<Action>()
        for (action in actions) {
          action?.let {
            if (!completedActionsSet.contains(action.id) && runningActions.indexOfValue(action) < 0) {
              candidates.add(action)
            }
          }
        }

        if (candidates.isNotEmpty()) {
          ActionQueueIntentService.appendActionsToQueue(getApplication(), candidates, this@ActionsViewModel)
        }
      }
    }
  }
}
