/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * The base implementation of [AndroidViewModel]
 *
 * @author Denys Bondarenko
 */
abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application)
