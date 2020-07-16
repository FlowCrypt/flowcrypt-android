/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.JavaEmailConstants
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

  val attsLiveData = roomDatabase.attachmentDao().getAttachmentsLD(msgEntity.email,
      msgEntity.folder, msgEntity.uid)

  val msgStatesLiveData = MutableLiveData<MessageState>()

  fun setSeenStatus(isSeen: Boolean) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().updateSuspend(msgEntity.copy(flags = if (isSeen) {
          if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
            msgEntity.flags
          } else {
            msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
          }
        } else {
          msgEntity.flags?.replace(MessageFlag.SEEN.value, "")
        }))
      }
    }
  }

  fun changeMsgState(newMsgState: MessageState) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        val candidate: MessageEntity = when (newMsgState) {
          MessageState.PENDING_MARK_READ -> {
            msgEntity.copy(
                state = newMsgState.value,
                flags = if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                  msgEntity.flags
                } else {
                  msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
                })
          }

          MessageState.PENDING_MARK_UNREAD -> {
            msgEntity.copy(
                state = newMsgState.value,
                flags = msgEntity.flags?.replace(MessageFlag.SEEN.value, ""))
          }

          else -> {
            msgEntity.copy(state = newMsgState.value)
          }
        }

        roomDatabase.msgDao().updateSuspend(candidate)
        msgStatesLiveData.postValue(newMsgState)
      }
    }
  }

  fun deleteMsg() {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().deleteSuspend(msgEntity)

        if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
          val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(msgEntity.email).size
          val outboxLabel = roomDatabase.labelDao().getLabelSuspend(msgEntity.email,
              JavaEmailConstants.FOLDER_OUTBOX)

          outboxLabel?.let {
            roomDatabase.labelDao().updateSuspend(it.copy(msgsCount = outgoingMsgCount))
          }
        }
      }
    }
  }
}