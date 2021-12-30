/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.base

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.jetpack.workmanager.BaseWorker

/**
 * @author Denis Bondarenko
 *         Date: 12/29/21
 *         Time: 4:56 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseMsgWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {

  protected suspend fun markMsgsWithAuthFailureState(
    roomDatabase: FlowCryptRoomDatabase,
    oldMessageState: MessageState
  ) {
    val account = roomDatabase.accountDao().getActiveAccountSuspend()
    roomDatabase.msgDao().changeMsgsStateSuspend(
      account = account?.email,
      label = JavaEmailConstants.FOLDER_OUTBOX,
      oldValue = oldMessageState.value,
      newValues = MessageState.AUTH_FAILURE.value
    )
  }
}
