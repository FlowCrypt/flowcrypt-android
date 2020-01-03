/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
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
class MessagesViewModel(application: Application) : BaseAndroidViewModel(application) {
  private var currentLocalFolder: LocalFolder? = null
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  val accountLiveData: LiveData<AccountEntity?> = liveData {
    val accountEntity = roomDatabase.accountDao().getActiveAccount()
    emit(accountEntity)
  }

  var msgsLiveData: LiveData<PagedList<MessageEntity>>? = null

  fun loadMsgs(lifecycleOwner: LifecycleOwner, localFolder: LocalFolder?, observer:
  Observer<PagedList<MessageEntity>>, boundaryCallback: PagedList
  .BoundaryCallback<MessageEntity>, forceClearCache: Boolean = false) {
    viewModelScope.launch {
      val label = if (localFolder?.searchQuery.isNullOrEmpty()) {
        localFolder?.fullName ?: ""
      } else {
        SearchMessagesActivity.SEARCH_FOLDER_NAME
      }

      if (forceClearCache) {
        roomDatabase.msgDao().delete(getActiveAccountSuspend()?.email, label)
      }

      val resetObserver = {
        val isSearchFolder = label == SearchMessagesActivity.SEARCH_FOLDER_NAME
        (currentLocalFolder?.fullName == localFolder?.fullName).not() || isSearchFolder
      }

      if (resetObserver()) {
        msgsLiveData?.removeObserver(observer)
        msgsLiveData = Transformations.switchMap(accountLiveData) {
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

  private suspend fun getActiveAccountSuspend(): AccountEntity? {
    return accountLiveData.value ?: return roomDatabase.accountDao().getActiveAccount()
  }

  fun deleteOutgoingMsg(messageEntity: MessageEntity) {
    val app = getApplication<Application>()

    viewModelScope.launch {
      val isMsgDeleted = with(messageEntity) {
        roomDatabase.msgDao().deleteOutgoingMsg(email, folder, uid) > 0
      }

      if (isMsgDeleted) {
        val account = getActiveAccountSuspend()
        val outgoingMsgCount = roomDatabase.msgDao().getOutboxMessages(account?.email).size
        val outboxLabel = roomDatabase.labelDao().getLabelSuspend(account?.email,
            JavaEmailConstants.FOLDER_OUTBOX)

        outboxLabel?.let {
          roomDatabase.labelDao().updateSuspend(it.copy(messageCount = outgoingMsgCount))
        }

        if (messageEntity.hasAttachments == true) {
          try {
            val parentDirName = messageEntity.attachmentsDirectory
            val dir = File(File(app.cacheDir, Constants.ATTACHMENTS_CACHE_DIR), parentDirName)
            FileAndDirectoryUtils.deleteDir(dir)
          } catch (e: IOException) {
            e.printStackTrace()
          }
        }
      }
    }
  }
}