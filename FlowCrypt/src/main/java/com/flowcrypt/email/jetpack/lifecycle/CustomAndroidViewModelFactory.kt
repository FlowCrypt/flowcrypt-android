/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.lifecycle

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * @author Denis Bondarenko
 *         Date: 6/30/22
 *         Time: 5:22 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class CustomAndroidViewModelFactory(app: Application) :
  ViewModelProvider.AndroidViewModelFactory(app) {
  override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
    return create(modelClass)
  }
}
