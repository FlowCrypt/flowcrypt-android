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
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 12:37 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application)