/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * @author Denis Bondarenko
 *         Date: 5/16/22
 *         Time: 10:38 AM
 *         E-mail: DenBond7@gmail.com
 */
object MessagesRepository {
  const val PAGE_SIZE = 15

  fun getMessagesPager(
    context: Context,
    localFolder: LocalFolder?
  ): Pager<Int, MessageEntity> {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    @OptIn(ExperimentalPagingApi::class)
    return Pager(
      config = PagingConfig(
        pageSize = PAGE_SIZE,
        enablePlaceholders = false
      ),
      pagingSourceFactory = {
        roomDatabase.msgDao().getMessagesPagingDataSourceFactory(
          account = localFolder?.account ?: "",
          folder = localFolder?.fullName ?: ""
        )
      },
      remoteMediator = MessagesRemoteMediator(context, roomDatabase, localFolder)
    )
  }
}
