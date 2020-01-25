/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * @author Denis Bondarenko
 *         Date: 1/25/20
 *         Time: 10:21 AM
 *         E-mail: DenBond7@gmail.com
 */
open class AccountViewModel(application: Application) : BaseAndroidViewModel(application) {
  protected val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  val accountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD()
}