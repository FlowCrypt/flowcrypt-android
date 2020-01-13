/*
 * © 2016-2020 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * @author Denis Bondarenko
 *         Date: 1/13/20
 *         Time: 12:32 PM
 *         E-mail: DenBond7@gmail.com
 */
class LabelsViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  val accountLiveData: LiveData<AccountEntity?> = liveData {
    val accountEntity = roomDatabase.accountDao().getActiveAccount()
    emit(accountEntity)
  }

  val labelsLiveData: LiveData<List<LabelEntity>> = Transformations.switchMap(accountLiveData) {
    val account = it?.email ?: ""
    roomDatabase.labelDao().getLabelsLD(account)
  }
}