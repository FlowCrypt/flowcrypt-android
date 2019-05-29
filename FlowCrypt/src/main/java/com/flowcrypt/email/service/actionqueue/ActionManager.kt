/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue

import android.content.Context
import android.util.LongSparseArray
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.service.actionqueue.actions.Action
import java.util.*

/**
 * This class is a manager which checks and runs [Action]
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 17:26
 * E-mail: DenBond7@gmail.com
 */

class ActionManager(context: Context) : ActionResultReceiver.ResultReceiverCallBack {
  private val context: Context = context.applicationContext
  private val runningActions: LongSparseArray<Action> = LongSparseArray()
  private val completedActionsSet: MutableSet<Long> = HashSet()
  private val actionQueueDaoSource: ActionQueueDaoSource = ActionQueueDaoSource()

  override fun onSuccess(action: Action?) {
    completedActionsSet.add(action!!.id)
    actionQueueDaoSource.deleteAction(context, action)
    runningActions.delete(action.id)
  }

  override fun onError(exception: Exception, action: Action?) {
    runningActions.delete(action!!.id)
  }

  /**
   * Check and add actions to the worker queue.
   *
   * @param account The [AccountDao] which has some actions.
   */
  fun checkAndAddActionsToQueue(account: AccountDao) {
    val actions = actionQueueDaoSource.getActions(context, account)
    val candidates = ArrayList<Action>()
    for (action in actions) {
      if (!completedActionsSet.contains(action.id) && runningActions.indexOfValue(action) < 0) {
        candidates.add(action)
      }
    }

    if (!candidates.isEmpty()) {
      ActionQueueIntentService.appendActionsToQueue(context, candidates, this)
    }
  }
}
