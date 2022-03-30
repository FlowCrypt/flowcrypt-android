/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.R
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    runChecking()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val superStartedState = super.onStartCommand(intent, flags, startId)
    startForeground(R.id.notification_id_passphrase_service, prepareNotification())
    return superStartedState
  }

  private fun runChecking() {
    setupFlowForPeriodicCheck()

    lifecycleScope.launch {
      repeatableActionFlow.collect {
        keysStorage.updatePassphrasesCache()
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

  private fun prepareNotification(): Notification {
    return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_SILENT)
      .setContentTitle(getString(R.string.active_passphrase_session))
      .setSmallIcon(R.drawable.ic_baseline_password_24dp)
      .setContentIntent(
        PendingIntent.getActivity(
          this,
          0,
          Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
          },
          PendingIntent.FLAG_IMMUTABLE
        )
      )
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .build()
  }

  companion object {
    /**
     * We will run checking every minute.
     */
    private val DELAY_TIMEOUT = TimeUnit.MINUTES.toMillis(1)

    /**
     * Start [PassPhrasesInRAMService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun start(context: Context) {
      val startEmailServiceIntent = Intent(context, PassPhrasesInRAMService::class.java)
      try {
        context.startForegroundService(startEmailServiceIntent)
      } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (e is ForegroundServiceStartNotAllowedException) {
            /*Because this service should be restarted by the system we can skip this exception.
             It seems this service was started manually after the app crash via the trigger.*/
            e.printStackTrace()
          }
        } else throw e
      }
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
