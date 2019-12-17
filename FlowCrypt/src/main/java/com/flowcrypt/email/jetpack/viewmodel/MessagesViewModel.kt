/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * @author Denis Bondarenko
 *         Date: 12/17/19
 *         Time: 4:37 PM
 *         E-mail: DenBond7@gmail.com
 */
class MessagesViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)
  val concertList: LiveData<PagedList<MessageEntity>> = roomDatabase.msgDao().msgs().toLiveData(pageSize = 20)
}