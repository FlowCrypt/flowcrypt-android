/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.ResultReceiver
import androidx.core.app.JobIntentService
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.util.*

/**
 * An [IntentService] subclass for handling asynchronous task requests ([Action]) in
 * a service on a separate handler thread.
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:54
 * E-mail: DenBond7@gmail.com
 */
class ActionQueueIntentService : JobIntentService() {
  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(TAG, "onStartCommand")
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onHandleWork(intent: Intent) {
    val intentAction = intent.action
    if (ACTION_RUN_ACTIONS == intentAction) {
      val actions = intent.getParcelableArrayListExtra<Action>(EXTRA_KEY_ACTIONS) ?: return
      val resultReceiver =
          intent.getParcelableExtra<ResultReceiver>(EXTRA_KEY_RESULTS_RECEIVER) ?: return

      if (actions.isNotEmpty()) {
        LogsUtil.d(TAG, "Received " + actions.size + " action(s) for run in the queue")
        for (action in actions) {
          action?.let {
            LogsUtil.d(TAG, "Run " + it.javaClass.simpleName)
            try {
              it.run(applicationContext)
              val successBundle = ActionResultReceiver.generateSuccessBundle(it)
              resultReceiver.send(ActionResultReceiver.RESULT_CODE_OK, successBundle)
              LogsUtil.d(TAG, it.javaClass.simpleName + ": success")
            } catch (e: Exception) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
              val errorBundle = ActionResultReceiver.generateErrorBundle(it, e)
              resultReceiver.send(ActionResultReceiver.RESULT_CODE_ERROR, errorBundle)
              LogsUtil.d(TAG, it.javaClass.simpleName + ": an error occurred")
            }
          }
        }
      }
    }
  }

  companion object {
    val ACTION_RUN_ACTIONS = GeneralUtil.generateUniqueExtraKey("ACTION_RUN_ACTIONS",
        ActionQueueIntentService::class.java)

    private val TAG = ActionQueueIntentService::class.java.simpleName
    private val EXTRA_KEY_ACTIONS = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACTIONS",
        ActionQueueIntentService::class.java)
    private val EXTRA_KEY_RESULTS_RECEIVER =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_RESULTS_RECEIVER", ActionQueueIntentService::class.java)

    /**
     * Starts this service to perform action [.ACTION_RUN_ACTIONS]. If the service is already performing a task
     * this action will be queued.
     *
     * @param context                Interface to global information about an application environment;
     * @param actions                A list of [Action] objects.
     * @param resultReceiverCallBack An implementation of [android.os.ResultReceiver].
     * @see IntentService
     */
    @JvmStatic
    fun appendActionsToQueue(context: Context, actions: ArrayList<Action>,
                             resultReceiverCallBack: ActionResultReceiver.ResultReceiverCallBack) {
      val resultReceiver = ActionResultReceiver(Handler(context.mainLooper))
      resultReceiver.setResultReceiverCallBack(resultReceiverCallBack)

      val intent = Intent(context, ActionQueueIntentService::class.java)
      intent.action = ACTION_RUN_ACTIONS
      intent.putExtra(EXTRA_KEY_ACTIONS, actions)
      intent.putExtra(EXTRA_KEY_RESULTS_RECEIVER, resultReceiver)

      enqueueWork(context, ActionQueueIntentService::class.java, JobIdManager.JOB_TYPE_ACTION_QUEUE, intent)
    }
  }
}
