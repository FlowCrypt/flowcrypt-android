/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * @author Denis Bondarenko
 *         Date: 1/13/20
 *         Time: 12:32 PM
 *         E-mail: DenBond7@gmail.com
 */
class LabelsViewModel(application: Application) : AccountViewModel(application) {
  val labelsLiveData: LiveData<List<LabelEntity>> = Transformations.switchMap(accountLiveData) {
    roomDatabase.labelDao().getLabelsLD(it?.email ?: "")
  }
}