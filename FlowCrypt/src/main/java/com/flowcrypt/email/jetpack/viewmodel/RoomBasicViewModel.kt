/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.room.RoomDatabase
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.service.actionqueue.actions.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This class helps to do background operations with [RoomDatabase]
 *
 * @author Denis Bondarenko
 *         Date: 3/13/20
 *         Time: 1:20 PM
 *         E-mail: DenBond7@gmail.com
 */
open class RoomBasicViewModel(application: Application) : BaseAndroidViewModel(application) {
  protected val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  fun addActionToQueue(action: Action) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        val entity = ActionQueueEntity.fromAction(action) ?: return@withContext
        roomDatabase.actionQueueDao().insertSuspend(entity)
      }
    }
  }
}