/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel

/**
 * @author Denis Bondarenko
 *         Date: 12/26/19
 *         Time: 4:48 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgDetailsViewModelFactory(val localFolder: LocalFolder, val msgEntity: MessageEntity,
                                 val application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return MsgDetailsViewModel(localFolder, msgEntity, application) as T
  }
}