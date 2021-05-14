/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.R
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 5/6/21
 *         Time: 9:56 AM
 *         E-mail: DenBond7@gmail.com
 */
class PassPhrasesInRAMService : BaseLifecycleService() {
  private lateinit var keysStorage: KeysStorage
  private lateinit var repeatableActionFlow: Flow<Long>

  override fun onCreate() {
    super.onCreate()
    keysStorage = KeysStorageImpl.getInstance(applicationContext)
    runAsForeground()
    runChecking()
  }

  private fun runAsForeground() {
    val pendingIntent: PendingIntent =
        Intent(this, EmailManagerActivity::class.java).let { notificationIntent ->
          PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

    val notification: Notification = Notification.Builder(this,
        NotificationChannelManager.CHANNEL_ID_SYSTEM)
        .setContentTitle("ContentTitle")
        .setContentText("ContentText")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentIntent(pendingIntent)
        .setTicker("Ticker text")
        .build()

    startForeground(100, notification)
  }

  private fun runChecking() {
    setupFlowForPeriodicCheck()

    lifecycleScope.launch {
      repeatableActionFlow.collect {
        keysStorage.updatePassPhrasesCache()
      }
    }
  }

  private fun setupFlowForPeriodicCheck() {
    repeatableActionFlow = flow {
      while (lifecycleScope.isActive) {
        emit(System.currentTimeMillis())
        delay(DELAY_TIMEOUT)
      }
    }.flowOn(Dispatchers.Default)
  }

  companion object {
    private val TAG = PassPhrasesInRAMService::class.java.simpleName

    /**
     * We will run checking every minute.
     */
    private val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(10)

    /**
     * Start [PassPhrasesInRAMService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun start(context: Context) {
      val startEmailServiceIntent = Intent(context, PassPhrasesInRAMService::class.java)
      context.startService(startEmailServiceIntent)
    }

    /**
     * Stop [PassPhrasesInRAMService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun stop(context: Context) {
      context.stopService(Intent(context, PassPhrasesInRAMService::class.java))
    }
  }
}