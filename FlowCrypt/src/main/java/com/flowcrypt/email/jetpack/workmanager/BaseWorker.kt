/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * @author Denis Bondarenko
 *         Date: 11/25/20
 *         Time: 5:11 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  abstract val useIndependentConnection: Boolean
}