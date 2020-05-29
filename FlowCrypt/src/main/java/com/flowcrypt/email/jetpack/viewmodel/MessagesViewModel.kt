/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.ui.activity.SearchMessagesActivity
import com.flowcrypt.email.util.FileAndDirectoryUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


/**
 * @author Denis Bondarenko
 *         Date: 12/17/19
 *         Time: 4:37 PM
 *         E-mail: DenBond7@gmail.com
 */
class MessagesViewModel(application: Application) : AccountViewModel(application) {
  private var currentLocalFolder: LocalFolder? = null

  val msgStatesLiveData = MutableLiveData<MessageState>()
  var msgsLiveData: LiveData<PagedList<MessageEntity>>? = null

  fun loadMsgs(lifecycleOwner: LifecycleOwner, localFolder: LocalFolder?,
               observer: Observer<PagedList<MessageEntity>>,
               boundaryCallback: PagedList.BoundaryCallback<MessageEntity>,
               forceClearFolderCache: Boolean = false,
               deleteAllMsgs: Boolean = false) {
    viewModelScope.launch {
      val label = if (localFolder?.searchQuery.isNullOrEmpty()) {
        localFolder?.fullName ?: ""
      } else {
        SearchMessagesActivity.SEARCH_FOLDER_NAME
      }

      val resetObserver = {
        val isSearchFolder = label == SearchMessagesActivity.SEARCH_FOLDER_NAME
        (currentLocalFolder?.fullName == localFolder?.fullName).not() || isSearchFolder ||
            deleteAllMsgs || forceClearFolderCache
      }

      if (resetObserver()) {
        msgsLiveData?.removeObserver(observer)
      }

      if (deleteAllMsgs) {
        roomDatabase.msgDao().deleteAllExceptOutgoing(getActiveAccountSuspend()?.email)
      } else if (forceClearFolderCache) {
        roomDatabase.msgDao().delete(getActiveAccountSuspend()?.email, label)
      }

      if (resetObserver()) {
        msgsLiveData = Transformations.switchMap(activeAccountLiveData) {
          val account = it?.email ?: ""
          roomDatabase.msgDao().getMessagesDataSourceFactory(account, label)
              .toLiveData(
                  config = Config(
                      pageSize = JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP / 3),
                  boundaryCallback = boundaryCallback)
        }

        msgsLiveData?.observe(lifecycleOwner, observer)
        currentLocalFolder = localFolder
      }
    }
  }

  fun cleanFolderCache(folderName: String?) {
    viewModelScope.launch {
      roomDatabase.msgDao().delete(getActiveAccountSuspend()?.email, folderName)
    }
  }

  fun deleteOutgoingMsgs(entities: Iterable<MessageEntity>) {
    val app = getApplication<Application>()

    viewModelScope.launch {
      var needUpdateOutboxLabel = false
      for (entity in entities) {
        val isMsgDeleted = with(entity) {
          roomDatabase.msgDao().deleteOutgoingMsg(email, folder, uid) > 0
        }

        if (isMsgDeleted) {
          needUpdateOutboxLabel = true
          if (entity.hasAttachments == true) {
            try {
              val parentDirName = entity.attachmentsDirectory
              parentDirName?.let {
                val dir = File(File(app.cacheDir, Constants.ATTACHMENTS_CACHE_DIR), it)
                FileAndDirectoryUtils.deleteDir(dir)
              }
            } catch (e: IOException) {
              e.printStackTrace()
            }
          }
        }
      }

      if (needUpdateOutboxLabel) {
        updateOutboxMsgsCount()
      }
    }
  }

  fun changeMsgsState(ids: Collection<Long>, localFolder: LocalFolder, newMsgState: MessageState,
                      notifyMsgStatesListener: Boolean = true) {
    viewModelScope.launch {
      val entities = roomDatabase.msgDao().getMsgsByIDSuspend(localFolder.account,
          localFolder.fullName, ids.map { it })

      if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
        if (newMsgState == MessageState.PENDING_DELETING) {
          deleteOutgoingMsgs(entities)
          return@launch
        }
      }

      val candidates = prepareCandidates(entities, newMsgState)
      roomDatabase.msgDao().updateSuspend(candidates)
      if (notifyMsgStatesListener) {
        msgStatesLiveData.postValue(newMsgState)
      }
    }
  }

  private suspend fun updateOutboxMsgsCount() {
    val account = getActiveAccountSuspend()
    val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsExceptSentSuspend(account?.email).size
    val outboxLabel = roomDatabase.labelDao().getLabelSuspend(account?.email,
        JavaEmailConstants.FOLDER_OUTBOX)

    outboxLabel?.let {
      roomDatabase.labelDao().updateSuspend(it.copy(msgsCount = outgoingMsgCount))
    }
  }

  private suspend fun getActiveAccountSuspend(): AccountEntity? {
    return activeAccountLiveData.value ?: return roomDatabase.accountDao().getActiveAccountSuspend()
  }

  private fun prepareCandidates(entities: Iterable<MessageEntity>, newMsgState: MessageState): Iterable<MessageEntity> {
    val candidates = mutableListOf<MessageEntity>()

    for (msgEntity in entities) {
      if (msgEntity.msgState == MessageState.SENDING || msgEntity.msgState == MessageState.SENT_WITHOUT_LOCAL_COPY) {
        continue
      }

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

      candidates.add(candidate)
    }

    return candidates
  }
}