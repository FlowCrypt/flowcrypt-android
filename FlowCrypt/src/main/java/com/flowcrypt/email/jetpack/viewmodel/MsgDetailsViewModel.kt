/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 12/26/19
 *         Time: 4:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgDetailsViewModel(val localFolder: LocalFolder, val msgEntity: MessageEntity,
                          application: Application) : BaseAndroidViewModel(application) {
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  var msgLiveData: LiveData<MessageEntity?> = roomDatabase.msgDao().getMsgLiveData(msgEntity.email,
      msgEntity.folder, msgEntity.uid)

  fun setSeenStatus(isSeen: Boolean) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().updateSuspend(msgEntity.copy(flags = if (isSeen) {
          MessageFlag.SEEN.value
        } else {
          msgEntity.flags?.replace(MessageFlag.SEEN.value, "")
        }))
      }
    }
  }

  fun updateMsgState(newMsgState: MessageState) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = newMsgState.value))
      }
    }
  }

  fun deleteMsg() {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().deleteSuspend(msgEntity)
      }
    }
  }
}