/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.service.actionqueue.actions.Action;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests ({@link Action}) in
 * a service on a separate handler thread.
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:54
 * E-mail: DenBond7@gmail.com
 */
public class ActionQueueIntentService extends JobIntentService {
  public static final String ACTION_RUN_ACTIONS = GeneralUtil.generateUniqueExtraKey("ACTION_RUN_ACTIONS",
      ActionQueueIntentService.class);

  private static final String TAG = ActionQueueIntentService.class.getSimpleName();
  private static final String EXTRA_KEY_ACTIONS = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACTIONS",
      ActionQueueIntentService.class);
  private static final String EXTRA_KEY_RESULTS_RECEIVER = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_RESULTS_RECEIVER", ActionQueueIntentService.class);

  /**
   * Starts this service to perform action {@link #ACTION_RUN_ACTIONS}. If the service is already performing a task
   * this action will be queued.
   *
   * @param context                Interface to global information about an application environment;
   * @param actions                A list of {@link Action} objects.
   * @param resultReceiverCallBack An implementation of {@link android.os.ResultReceiver}.
   * @see IntentService
   */

  public static void appendActionsToQueue(Context context, ArrayList<Action> actions,
                                          ActionResultReceiver.ResultReceiverCallBack resultReceiverCallBack) {
    ActionResultReceiver resultReceiver = new ActionResultReceiver(new Handler(context.getMainLooper()));
    resultReceiver.setResultReceiverCallBack(resultReceiverCallBack);

    Intent intent = new Intent(context, ActionQueueIntentService.class);
    intent.setAction(ACTION_RUN_ACTIONS);
    intent.putExtra(EXTRA_KEY_ACTIONS, actions);
    intent.putExtra(EXTRA_KEY_RESULTS_RECEIVER, resultReceiver);

    enqueueWork(context, ActionQueueIntentService.class, JobIdManager.JOB_TYPE_ACTION_QUEUE, intent);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");
  }

  @Override
  public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
    Log.d(TAG, "onStartCommand");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    final String intentAction = intent.getAction();
    if (ACTION_RUN_ACTIONS.equals(intentAction)) {
      final ArrayList<Action> actions = intent.getParcelableArrayListExtra(EXTRA_KEY_ACTIONS);
      final ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_KEY_RESULTS_RECEIVER);

      if (actions != null && !actions.isEmpty()) {
        Log.d(TAG, "Received " + actions.size() + " action(s) for run in the queue");
        for (Action action : actions) {
          if (action != null) {
            Log.d(TAG, "Run " + action.getClass().getSimpleName());
            try {
              action.run(getApplicationContext());
              Bundle successBundle = ActionResultReceiver.generateSuccessBundle(action);
              resultReceiver.send(ActionResultReceiver.RESULT_CODE_OK, successBundle);
              Log.d(TAG, action.getClass().getSimpleName() + ": success");
            } catch (Exception e) {
              e.printStackTrace();
              Bundle errorBundle = ActionResultReceiver.generateErrorBundle(action, e);
              resultReceiver.send(ActionResultReceiver.RESULT_CODE_ERROR, errorBundle);
              Log.d(TAG, action.getClass().getSimpleName() + ": an error occurred");
            }
          }
        }
      }
    }
  }
}
