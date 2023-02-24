/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.flowcrypt.email.util.LogsUtil

/**
 * @author Denys Bondarenko
 */
abstract class BaseLifecycleService : LifecycleService() {
  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(javaClass.simpleName, "onCreate")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(
      javaClass.simpleName,
      "onStartCommand |intent =$intent |flags = $flags |startId = $startId"
    )
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(javaClass.simpleName, "onDestroy")
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)
    LogsUtil.d(javaClass.simpleName, "onRebind:$intent")
  }

  override fun onUnbind(intent: Intent): Boolean {
    LogsUtil.d(javaClass.simpleName, "onUnbind:$intent")
    return super.onUnbind(intent)
  }
}
