/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.flowcrypt.email.jetpack.viewmodel.PgpKeyDetailsViewModel

/**
 * @author Denis Bondarenko
 *         Date: 5/24/21
 *         Time: 5:35 PM
 *         E-mail: DenBond7@gmail.com
 */
class PgpKeyDetailsViewModelFactory(
  val fingerprint: String?,
  val application: Application
) :
  ViewModelProvider.AndroidViewModelFactory(application) {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return PgpKeyDetailsViewModel(fingerprint, application) as T
  }
}
