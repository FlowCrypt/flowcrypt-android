/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.work.WorkManager
import com.flowcrypt.email.R
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.hasActiveConnection
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.viewmodel.RoomBasicViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:21.
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseActivity : AppCompatActivity() {
  protected val roomBasicViewModel: RoomBasicViewModel by viewModels()
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val tag: String = javaClass.simpleName
  protected var activeAccount: AccountEntity? = null
  private lateinit var connectionLifecycleObserver: ConnectionLifecycleObserver

  val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  var snackBar: Snackbar? = null
    private set
  var toolbar: Toolbar? = null
    private set
  var appBarLayout: AppBarLayout? = null
    private set

  /**
   * This method can used to change "HomeAsUpEnabled" behavior.
   *
   * @return true if we want to show "HomeAsUpEnabled", false otherwise.
   */
  abstract val isDisplayHomeAsUpEnabled: Boolean

  /**
   * Get the content view resources id. This method must return an resources id of a layout
   * if we want to show some UI.
   *
   * @return The content view resources id.
   */
  abstract val contentViewResourceId: Int

  /**
   * Get root view which will be used for show Snackbar.
   */
  abstract val rootView: View

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    connectionLifecycleObserver = ConnectionLifecycleObserver(this)
    lifecycle.addObserver(connectionLifecycleObserver)

    LogsUtil.d(tag, "onCreate")
    if (contentViewResourceId != 0) {
      setContentView(contentViewResourceId)
      initScreenViews()
    }

    initAccountViewModel()
  }

  public override fun onStart() {
    super.onStart()
    LogsUtil.d(tag, "onStart")
  }

  public override fun onResume() {
    super.onResume()
    LogsUtil.d(tag, "onResume")
  }

  override fun onPause() {
    super.onPause()
    LogsUtil.d(tag, "onPause")
  }

  public override fun onStop() {
    super.onStop()
    LogsUtil.d(tag, "onStop")
  }

  public override fun onDestroy() {
    super.onDestroy()
    lifecycle.removeObserver(connectionLifecycleObserver)
    LogsUtil.d(tag, "onDestroy")
    countingIdlingResource.shutdown()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_base, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> if (isDisplayHomeAsUpEnabled) {
        finish()
        true
      } else {
        super.onOptionsItemSelected(item)
      }

      R.id.menuActionHelp -> {
        FeedbackActivity.show(this)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   * @param duration    How long to display the message.
   */
  fun showInfoSnackbar(
    view: View?,
    messageText: String?,
    duration: Int = Snackbar.LENGTH_INDEFINITE
  ) {
    view?.let {
      snackBar = Snackbar.make(it, messageText ?: "", duration).setAction(android.R.string.ok) { }
      snackBar?.show()
    }
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  fun showSnackbar(
    view: View, messageText: String, buttonName: String,
    onClickListener: View.OnClickListener
  ) {
    showSnackbar(view, messageText, buttonName, Snackbar.LENGTH_INDEFINITE, onClickListener)
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  fun showSnackbar(
    view: View, messageText: String, buttonName: String, duration: Int,
    onClickListener: View.OnClickListener
  ) {
    snackBar = Snackbar.make(view, messageText, duration).setAction(buttonName, onClickListener)
    snackBar?.show()
  }

  fun dismissSnackBar() {
    snackBar?.dismiss()
  }

  protected open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {

  }

  protected fun isConnected(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      connectionLifecycleObserver.connectionLiveData.value ?: false
    } else {
      hasActiveConnection()
    }
  }

  protected fun removeAccountFromAccountManager(accountEntity: AccountEntity?) {
    val accountManager = AccountManager.get(this)
    accountManager.accounts.firstOrNull { it.name == accountEntity?.email }?.let { account ->
      if (account.type.equals(FlowcryptAccountAuthenticator.ACCOUNT_TYPE, ignoreCase = true)) {
        accountManager.removeAccountExplicitly(account)
      }
    }
  }

  fun logout() {
    lifecycleScope.launch {
      activeAccount?.let { accountEntity ->
        countingIdlingResource.incrementSafely()
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)

        val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
        roomDatabase.accountDao().logout(accountEntity)
        removeAccountFromAccountManager(accountEntity)

        //todo-denbond7 Improve this via onDelete = ForeignKey.CASCADE
        //remove all info about the given account from the local db
        roomDatabase.msgDao().deleteByEmailSuspend(accountEntity.email)
        roomDatabase.attachmentDao().deleteByEmailSuspend(accountEntity.email)

        val newActiveAccount = roomDatabase.accountDao().getActiveAccountSuspend()
        if (newActiveAccount == null) {
          roomDatabase.recipientDao().deleteAll()
          stopService(Intent(applicationContext, IdleService::class.java))
          val intent = Intent(applicationContext, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          startActivity(intent)
          finish()
        }

        countingIdlingResource.decrementSafely()
      }
    }
  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this) {
      activeAccount = it
      onAccountInfoRefreshed(activeAccount)
    }
  }

  private fun initScreenViews() {
    appBarLayout = findViewById(R.id.appBarLayout)
    setupToolbar()
  }

  private fun setupToolbar() {
    toolbar = findViewById(R.id.toolbar)
    toolbar?.let { setSupportActionBar(it) }
    supportActionBar?.setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled)
  }
}
