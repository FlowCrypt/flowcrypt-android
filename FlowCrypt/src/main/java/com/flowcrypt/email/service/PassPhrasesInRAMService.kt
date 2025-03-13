/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.toast
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
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
class PassPhrasesInRAMService : BaseLifecycleService() {
  private lateinit var keysStorage: KeysStorage
  private val repeatableActionFlow: Flow<Long> = setupFlowForPeriodicCheck()

  override fun onCreate() {
    super.onCreate()
    keysStorage = KeysStorageImpl.getInstance(applicationContext)
    subscribeToPassphrasesUpdates()
    runChecking()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val superStartedState = super.onStartCommand(intent, flags, startId)
    when (intent?.action) {
      ACTION_END_PASSPHRASE_SESSION -> {
        keysStorage.clearPassphrasesCache()
        updateNotification(useActionButton = false)
      }

      else -> {
        val notification = prepareNotification(
          useActionButton = keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          startForeground(
            R.id.notification_id_passphrase_service, notification,
            //https://developer.android.com/about/versions/14/changes/fgs-types-required#permission-for-fgs-type
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
          )
        } else {
          startForeground(
            R.id.notification_id_passphrase_service, notification
          )
        }
      }
    }
    return superStartedState
  }

  private fun subscribeToPassphrasesUpdates() {
    lifecycleScope.launch {
      keysStorage.getPassPhrasesUpdatesFlow().collect {
        updateNotification(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))
      }
    }
  }

  private fun runChecking() {
    lifecycleScope.launch {
      repeatableActionFlow.collect {
        keysStorage.updatePassphrasesCache()
      }
    }
  }

  private fun setupFlowForPeriodicCheck() = flow {
    while (lifecycleScope.isActive) {
      emit(System.currentTimeMillis())
      delay(DELAY_TIMEOUT)
    }
  }.flowOn(Dispatchers.Default)

  private fun prepareNotification(useActionButton: Boolean = true): Notification {
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
      .apply {
        if (useActionButton) {
          addAction(genEndPassPhraseSessionAction())
        }
      }
      .build()
  }

  private fun genEndPassPhraseSessionAction(): NotificationCompat.Action {
    val intent = Intent(applicationContext, PassPhrasesInRAMService::class.java)
    intent.action = ACTION_END_PASSPHRASE_SESSION

    val pendingIntent = PendingIntent.getService(
      applicationContext,
      Random().nextInt(),
      intent,
      PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Action.Builder(
      0,
      getString(R.string.end_pass_phrase_session),
      pendingIntent
    ).build()
  }

  fun updateNotification(useActionButton: Boolean) {
    val isAndroidTiramisuOrHigh = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (isAndroidTiramisuOrHigh && ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      toast(getString(R.string.cannot_show_notifications_without_permission))
      return
    }

    NotificationManagerCompat.from(applicationContext).notify(
      R.id.notification_id_passphrase_service, prepareNotification(
        useActionButton = useActionButton
      )
    )
  }

  companion object {
    const val ACTION_END_PASSPHRASE_SESSION =
      BuildConfig.APPLICATION_ID + ".ACTION_END_PASSPHRASE_SESSION"

    /**
     * We will run checking every minute.
     */
    val DELAY_TIMEOUT = TimeUnit.MINUTES.toMillis(1)

    /**
     * Start [PassPhrasesInRAMService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun start(context: Context) {
      val startServiceIntent = Intent(context, PassPhrasesInRAMService::class.java)
      try {
        context.startForegroundService(startServiceIntent)
      } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (e is ForegroundServiceStartNotAllowedException) {
            /*Because this service should be restarted by the system we can skip this exception.
             It seems this service was started manually after the app crash via the trigger.*/
            e.printStackTraceIfDebugOnly()
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
      val intent = Intent(context, PassPhrasesInRAMService::class.java)
      context.stopService(intent)
    }
  }
}
