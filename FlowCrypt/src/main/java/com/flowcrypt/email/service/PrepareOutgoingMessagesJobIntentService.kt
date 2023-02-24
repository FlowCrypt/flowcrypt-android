/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.extensions.android.content.getParcelableExtraViaExt
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil

/**
 * This service creates a new outgoing message using the given [OutgoingMessageInfo].
 *
 * @author Denys Bondarenko
 */
class PrepareOutgoingMessagesJobIntentService : JobIntentService() {
  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
  }

  override fun onStopCurrentWork(): Boolean {
    LogsUtil.d(TAG, "onStopCurrentWork")
    return super.onStopCurrentWork()
  }

  override fun onHandleWork(intent: Intent) {
    LogsUtil.d(TAG, "onHandleWork")
    val originalOutgoingMsgInfo =
      intent.getParcelableExtraViaExt<OutgoingMessageInfo>(EXTRA_KEY_OUTGOING_MESSAGE_INFO)
        ?: return
    ProcessingOutgoingMessageInfoHelper.process(applicationContext, originalOutgoingMsgInfo)
  }

  companion object {
    private val EXTRA_KEY_OUTGOING_MESSAGE_INFO =
      GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_OUTGOING_MESSAGE_INFO",
        PrepareOutgoingMessagesJobIntentService::class.java
      )
    private val TAG = PrepareOutgoingMessagesJobIntentService::class.java.simpleName

    /**
     * Enqueue a new task for [PrepareOutgoingMessagesJobIntentService].
     *
     * @param context         Interface to global information about an application environment.
     * @param outgoingMsgInfo [OutgoingMessageInfo] which contains information about an outgoing message.
     */
    fun enqueueWork(context: Context, outgoingMsgInfo: OutgoingMessageInfo?) {
      if (outgoingMsgInfo != null) {
        val intent = Intent(context, PrepareOutgoingMessagesJobIntentService::class.java)
        intent.putExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO, outgoingMsgInfo)

        enqueueWork(
          context, PrepareOutgoingMessagesJobIntentService::class.java,
          JobIdManager.JOB_TYPE_PREPARE_OUT_GOING_MESSAGE, intent
        )
      }
    }
  }
}
