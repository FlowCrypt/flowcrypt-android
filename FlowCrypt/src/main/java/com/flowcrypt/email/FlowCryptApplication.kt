/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.jetpack.workmanager.MsgsCacheCleanerWorker
import com.flowcrypt.email.jetpack.workmanager.sync.SyncInboxWorker
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.service.PassPhrasesInRAMService
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.pgpainless.PGPainless
import org.pgpainless.policy.Policy.HashAlgorithmPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * The application class for FlowCrypt. Base class for maintaining global application state. The production version.
 *
 * @author Denys Bondarenko
 */
class FlowCryptApplication : Application(), Configuration.Provider {

  val appForegroundedObserver = AppForegroundedObserver()

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setMinimumLoggingLevel(
        if (GeneralUtil.isDebugBuild()) {
          Log.DEBUG
        } else {
          Log.ERROR
        }
      ).build()

  override fun onCreate() {
    super.onCreate()
    ProcessLifecycleOwner.get().lifecycle.addObserver(appForegroundedObserver)

    setupGlobalSettingsForJavaMail()
    setupPGPainless()
    setupKeysStorage()
    initPerInstallationSharedPrefs()
    MsgsCacheManager.init(this)
    NotificationChannelManager.registerNotificationChannels(this)
    IMAPStoreManager.init(this)
    SyncInboxWorker.enqueuePeriodic(this)
    enqueueMsgsCacheCleanerWorker()
    FlavorSettings.configure(this)
    clearGlideDiskCache()
    cleanCacheForPgpOnlyMode()
  }

  private fun setupPGPainless() {
    enableDeprecatedSHA1ForPGPainlessPolicy()

    //https://github.com/FlowCrypt/flowcrypt-android/issues/2111
    PGPainless.getPolicy().enableKeyParameterValidation = true
  }

  private fun setupGlobalSettingsForJavaMail() {
    //based on https://github.com/FlowCrypt/flowcrypt-android/issues/1553
    System.setProperty("mail.mime.base64.ignoreerrors", "true")
    System.setProperty("mail.mime.parameters.strict", "false")
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initACRA()
  }

  /**
   * Allow sha1 for all builds except enterprise. It's a temporary solution.
   * More details here https://github.com/FlowCrypt/flowcrypt-android/issues/1478 and here
   * https://github.com/pgpainless/pgpainless/issues/158
   */
  private fun enableDeprecatedSHA1ForPGPainlessPolicy() {
    @Suppress("KotlinConstantConditions")
    if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) {
      PGPainless.getPolicy().dataSignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022SignatureHashAlgorithmPolicy()

      PGPainless.getPolicy().certificationSignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022SignatureHashAlgorithmPolicy()
    } else {
      PGPainless.getPolicy().dataSignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022RevocationSignatureHashAlgorithmPolicy()

      PGPainless.getPolicy().certificationSignatureHashAlgorithmPolicy =
        HashAlgorithmPolicy.static2022RevocationSignatureHashAlgorithmPolicy()
    }
  }

  private fun setupKeysStorage() {
    val keysStorage = KeysStorageImpl.getInstance(this)
    keysStorage.secretKeyRingsLiveData.observeForever {
      val hasTemporaryPassPhrases =
        keysStorage.getRawKeys().any { it.passphraseType == KeyEntity.PassphraseType.RAM }
      if (!hasTemporaryPassPhrases) {
        PassPhrasesInRAMService.stop(this)
      }
    }
  }

  private fun initACRA() {
    if (GeneralUtil.isDebugBuild()) {
      val isAcraEnabled = SharedPreferencesHelper.getBoolean(
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this),
        key = Constants.PREF_KEY_IS_ACRA_ENABLED,
        defaultValue = BuildConfig.IS_ACRA_ENABLED
      )
      if (isAcraEnabled) {
        setupACRA()
      }
    } else {
      setupACRA()
    }
  }

  private fun setupACRA() {
    @Suppress("KotlinConstantConditions")
    if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) return

    initAcra {
      reportFormat = StringFormat.JSON
      buildConfigClass = BuildConfig::class.java
      reportContent = listOf(
        ReportField.ANDROID_VERSION,
        ReportField.APP_VERSION_CODE,
        ReportField.APP_VERSION_NAME,
        ReportField.AVAILABLE_MEM_SIZE,
        ReportField.BRAND,
        ReportField.BUILD,
        ReportField.BUILD_CONFIG,
        ReportField.CRASH_CONFIGURATION,
        ReportField.CUSTOM_DATA,
        ReportField.DEVICE_FEATURES,
        ReportField.DISPLAY,
        ReportField.DUMPSYS_MEMINFO,
        ReportField.ENVIRONMENT,
        ReportField.FILE_PATH,
        ReportField.INITIAL_CONFIGURATION,
        ReportField.INSTALLATION_ID,
        ReportField.IS_SILENT,
        ReportField.PACKAGE_NAME,
        ReportField.PHONE_MODEL,
        ReportField.PRODUCT,
        ReportField.REPORT_ID,
        ReportField.STACK_TRACE,
        ReportField.TOTAL_MEM_SIZE,
        ReportField.USER_APP_START_DATE,
        ReportField.USER_CRASH_DATE,
        ReportField.USER_EMAIL
      )

      httpSender {
        uri = BuildConfig.SHARED_TENANT_FES_URL + "api/v1/log-collector/exception"
        httpMethod = HttpSender.Method.POST
        //need to disable the default http sender as we use a custom one
        enabled = false
      }
    }

    putCustomDataToACRAReports()
  }

  private fun putCustomDataToACRAReports() {
    val installVersion = SharedPreferencesHelper.getString(
      PreferenceManager.getDefaultSharedPreferences(this),
      Constants.PREF_KEY_INSTALL_VERSION, "unknown"
    ) ?: "unknown"

    ACRA.errorReporter.putCustomData(
      key = Constants.PREF_KEY_INSTALL_VERSION.uppercase(),
      value = installVersion
    )
  }

  private fun initPerInstallationSharedPrefs() {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    if (sharedPreferences.all.isEmpty()) {
      if (!sharedPreferences.contains(Constants.PREF_KEY_INSTALL_VERSION)) {
        sharedPreferences
          .edit()
          .putString(Constants.PREF_KEY_INSTALL_VERSION, BuildConfig.VERSION_NAME)
          .apply()
      }
    }
  }

  private fun enqueueMsgsCacheCleanerWorker() {
    val periodicWorkRequestBuilder =
      PeriodicWorkRequestBuilder<MsgsCacheCleanerWorker>(1, TimeUnit.DAYS)

    val calendar = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 5)
    }

    val workRequest = periodicWorkRequestBuilder
      .setInitialDelay(calendar.timeInMillis, TimeUnit.MILLISECONDS)
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      MsgsCacheCleanerWorker.NAME,
      ExistingPeriodicWorkPolicy.UPDATE, workRequest
    )
  }

  /**
   * To prevent after session issues we clear images local cache
   */
  @OptIn(DelicateCoroutinesApi::class)
  private fun clearGlideDiskCache() {
    GlobalScope.launch {
      withContext(Dispatchers.IO) {
        Glide.get(this@FlowCryptApplication).clearDiskCache()
      }
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun cleanCacheForPgpOnlyMode() {
    GlobalScope.launch {
      withContext(Dispatchers.IO) {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@FlowCryptApplication)
        val activeAccountEntity =
          roomDatabase.accountDao().getActiveAccountSuspend() ?: return@withContext

        if (activeAccountEntity.showOnlyEncrypted == true) {
          roomDatabase.msgDao().deleteAllExceptOutgoingAndDraft(
            context = this@FlowCryptApplication,
            accountEntity = activeAccountEntity
          )
        }
      }
    }
  }

  class AppForegroundedObserver : DefaultLifecycleObserver {
    val isAppForegrounded: Boolean
      get() {
        return isAppForegroundedInternal
      }
    private var isAppForegroundedInternal = false
    override fun onStart(owner: LifecycleOwner) {
      isAppForegroundedInternal = true
    }

    override fun onStop(owner: LifecycleOwner) {
      isAppForegroundedInternal = false
    }
  }
}
