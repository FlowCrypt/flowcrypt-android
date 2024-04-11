/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * @author Denys Bondarenko
 */
class MessagesViewPagerViewModel(
  private val initialMessageEntityId: Long,
  private val localFolder: LocalFolder,
  application: Application
) : AccountViewModel(application) {
  private val initialLiveData: LiveData<Result<List<MessageEntity>>> =
    activeAccountLiveData.switchMap { accountEntity ->
      liveData {
        if (accountEntity != null) {
          emit(Result.loading())

          val middleMessageEntity =
            roomDatabase.msgDao().getMsgById(initialMessageEntityId)

          if (middleMessageEntity != null) {
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
          } else {
            emit(Result.success(emptyList()))
          }
        } else {
          emit(Result.success(emptyList()))
        }
      }
    }

  private val manuallySelectedMessageEntity: MutableLiveData<MessageEntity> = MutableLiveData()

  private val fetchLiveData: LiveData<Result<List<MessageEntity>>> =
    manuallySelectedMessageEntity.switchMap { messageEntity ->
      liveData {
        emit(Result.loading())
        val activeAccount = getActiveAccountSuspend()
        if (activeAccount != null) {
          emit(
            Result.success(
              roomDatabase.msgDao()
                .getMessagesForViewPager(
                  activeAccount.email,
                  localFolder.fullName,
                  messageEntity.receivedDate ?: 0,
                  PAGE_SIZE / 2
                )
            )
          )
        } else {
          emit(Result.success(emptyList()))
        }
      }
    }

  val messageEntitiesLiveData = MediatorLiveData<Result<List<MessageEntity>>>()

  init {
    messageEntitiesLiveData.addSource(initialLiveData) {
      messageEntitiesLiveData.value = it
    }

    messageEntitiesLiveData.addSource(fetchLiveData) {
      messageEntitiesLiveData.value = it
    }
  }

  fun onItemSelected(messageEntity: MessageEntity) {
    val position = messageEntitiesLiveData.value?.data?.indexOf(messageEntity) ?: return
    val listSize = messageEntitiesLiveData.value?.data?.size
    if (listSize == null || listSize == 0) {
      return
    }

    if (position <= listSize / 4 || position >= (listSize - listSize / 4)) {
      manuallySelectedMessageEntity.value = messageEntity
    }
  }

  companion object {
    const val PAGE_SIZE = 10
  }
}