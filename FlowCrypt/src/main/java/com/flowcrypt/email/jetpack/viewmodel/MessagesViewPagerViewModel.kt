/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * @author Denys Bondarenko
 */
class MessagesViewPagerViewModel(
  private val messageEntityId: Long,
  private val localFolder: LocalFolder,
  application: Application
) :
  AccountViewModel(application) {
  private val initialLiveData: LiveData<Result<List<MessageEntity>>> =
    activeAccountLiveData.switchMap { accountEntity ->
      liveData {
        if (accountEntity != null) {
          val middleMessageEntity =
            roomDatabase.msgDao().getMsgById(messageEntityId) ?: return@liveData

          emit(
            Result.success(
              roomDatabase.msgDao()
                .getMessagesForViewPager(
                  accountEntity.email,
                  localFolder.fullName,
                  middleMessageEntity.receivedDate ?: 0,
                  PAGE_SIZE / 2
                )
            )
          )
        }
      }
    }

  val messageEntitiesLiveData = MediatorLiveData<Result<List<MessageEntity>>>()

  init {
    messageEntitiesLiveData.addSource(initialLiveData) {
      messageEntitiesLiveData.value = it
    }
  }

  companion object {
    private const val PAGE_SIZE = 30
  }
}