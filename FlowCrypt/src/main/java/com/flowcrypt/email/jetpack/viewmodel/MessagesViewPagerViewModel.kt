/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * @author Denys Bondarenko
 */
class MessagesViewPagerViewModel(
  private val initialMessageEntityId: Long,
  private val sortedEntityIdListForThread: List<Long>? = null,
  private val localFolder: LocalFolder,
  application: Application
) : AccountViewModel(application) {

  private val isThreadMode: Boolean = sortedEntityIdListForThread?.isNotEmpty() == true

  val initialLiveData: LiveData<Result<List<MessageEntity>>> =
    activeAccountLiveData.switchMap { accountEntity ->
      liveData {
        if (accountEntity != null) {
          val middleMessageEntity =
            roomDatabase.msgDao().getMsgById(initialMessageEntityId)

          if (middleMessageEntity != null) {
            emit(
              Result.success(listOf(middleMessageEntity))
            )
          } else {
            emit(
              Result.exception(
                IllegalStateException(
                  "MessageEntity with id = $initialMessageEntityId not found"
                )
              )
            )
          }
        } else {
          emit(Result.exception(IllegalStateException("account is null")))
        }
      }
    }

  private val manuallySelectedMessageEntity: MutableLiveData<MessageEntity> = MutableLiveData()

  val messageEntitiesLiveData: LiveData<Result<List<MessageEntity>>> =
    manuallySelectedMessageEntity.switchMap { messageEntity ->
      liveData {
        emit(Result.loading())
        val activeAccount = getActiveAccountSuspend()
        if (activeAccount != null) {
          val result = if (isThreadMode) {
            val cachedMessages =
              roomDatabase.msgDao().getMessagesByIDs(sortedEntityIdListForThread ?: emptyList())
            val sortedCachedMessages =
              arrayOfNulls<MessageEntity>(cachedMessages.size).toMutableList().apply {
                cachedMessages.forEach { messageEntity ->
                  add(sortedEntityIdListForThread?.indexOf(messageEntity.id) ?: 0, messageEntity)
                }
              }.filterNotNull()

            Result.success(sortedCachedMessages)
          } else {
            Result.success(
              roomDatabase.msgDao()
                .getMessagesForViewPager(
                  account = activeAccount.email,
                  folder = if (localFolder.searchQuery.isNullOrEmpty()) {
                    localFolder.fullName
                  } else {
                    JavaEmailConstants.FOLDER_SEARCH
                  },
                  date = messageEntity.receivedDate ?: 0,
                  limit = PAGE_SIZE / 2
                )
            )
          }

          emit(result)
        } else {
          emit(Result.success(emptyList()))
        }
      }
    }

  fun onItemSelected(messageEntity: MessageEntity) {
    if (messageEntitiesLiveData.value?.data == null){
      manuallySelectedMessageEntity.value = messageEntity
      return
    }

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
    const val PAGE_SIZE = 30
  }
}