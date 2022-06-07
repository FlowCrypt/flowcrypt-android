/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.extensions.goAsync
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * This [BroadcastReceiver] will be used by [MessagesNotificationManager] to mark messages as old.
 *
 * @author Denis Bondarenko
 * Date: 03.07.2018
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
class MarkMessagesAsOldBroadcastReceiver : BroadcastReceiver() {

  @OptIn(DelicateCoroutinesApi::class)
  override fun onReceive(context: Context, intent: Intent?) {
    LogsUtil.d(TAG, "onReceive")
    if (intent == null || ACTION_MARK_MESSAGES_AS_OLD != intent.action) {
      return
    }

    val email = intent.getStringExtra(EXTRA_KEY_EMAIL)
    val label = intent.getStringExtra(EXTRA_KEY_LABEL)

    val uidList = intent.getStringArrayListExtra(EXTRA_KEY_UID_LIST)?.map { it.toLong() } ?: return

    goAsync {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      roomDatabase.msgDao().setOldStatus(email, label, uidList)
    }
  }

  companion object {
    val ACTION_MARK_MESSAGES_AS_OLD = GeneralUtil.generateUniqueExtraKey(
      "ACTION_MARK_MESSAGES_AS_OLD", MarkMessagesAsOldBroadcastReceiver::class.java
    )
    val EXTRA_KEY_UID_LIST = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_UID_LIST", MarkMessagesAsOldBroadcastReceiver::class.java
    )
    val EXTRA_KEY_EMAIL = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_EMAIL", MarkMessagesAsOldBroadcastReceiver::class.java
    )
    val EXTRA_KEY_LABEL = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_LABEL", MarkMessagesAsOldBroadcastReceiver::class.java
    )

    private val TAG = MarkMessagesAsOldBroadcastReceiver::class.java.simpleName
  }
}
