/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue;

import android.content.Context;
import android.util.LongSparseArray;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource;
import com.flowcrypt.email.service.actionqueue.actions.Action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is a manager which checks and runs {@link Action}
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 17:26
 * E-mail: DenBond7@gmail.com
 */

public class ActionManager implements ActionResultReceiver.ResultReceiverCallBack {
  private Context context;
  private LongSparseArray<Action> runningActions;
  private Set<Long> completedActionsSet;
  private ActionQueueDaoSource actionQueueDaoSource;

  public ActionManager(Context context) {
    this.context = context.getApplicationContext();
    this.actionQueueDaoSource = new ActionQueueDaoSource();
    this.runningActions = new LongSparseArray<>();
    this.completedActionsSet = new HashSet<>();
  }

  @Override
  public void onSuccess(Action action) {
    completedActionsSet.add(action.getId());
    actionQueueDaoSource.deleteAction(context, action);
    if (runningActions != null) {
      runningActions.delete(action.getId());
    }
  }

  @Override
  public void onError(Exception exception, Action action) {
    if (runningActions != null) {
      runningActions.delete(action.getId());
    }
  }

  /**
   * Check and add actions to the worker queue.
   *
   * @param account The {@link AccountDao} which has some actions.
   */
  public void checkAndAddActionsToQueue(AccountDao account) {
    List<Action> actions = actionQueueDaoSource.getActions(context, account);
    ArrayList<Action> candidates = new ArrayList<>();
    for (Action action : actions) {
      if (!completedActionsSet.contains(action.getId()) && runningActions.indexOfValue(action) < 0) {
        candidates.add(action);
      }
    }

    if (!candidates.isEmpty()) {
      ActionQueueIntentService.appendActionsToQueue(context, candidates, this);
    }
  }
}
