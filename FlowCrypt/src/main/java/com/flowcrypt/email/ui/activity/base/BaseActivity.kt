/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.hasActiveConnection
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.viewmodel.RoomBasicViewModel
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.service.BaseService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.idling.NodeIdlingResource
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:21.
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseActivity : AppCompatActivity(), BaseService.OnServiceCallback {
  protected val roomBasicViewModel: RoomBasicViewModel by viewModels()
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val tag: String = javaClass.simpleName
  protected var activeAccount: AccountEntity? = null
  protected var isAccountInfoReceived = false
  protected lateinit var connectionLifecycleObserver: ConnectionLifecycleObserver

  val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(GeneralUtil.genIdlingResourcesName(javaClass::class.java), GeneralUtil.isDebugBuild())

  val nodeIdlingResource: NodeIdlingResource = NodeIdlingResource()

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

  val replyMessengerName: String
    get() = javaClass.simpleName + "_" + hashCode()

  val isNodeReady: Boolean
    get() = if (Node.getInstance(application).liveData.value == null) {
      false
    } else {
      Node.getInstance(application).liveData.value?.isReady ?: false
    }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {

  }

  override fun onCanceled(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    connectionLifecycleObserver = ConnectionLifecycleObserver(this)
    lifecycle.addObserver(connectionLifecycleObserver)

    registerNodeIdlingResources()
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
  fun showInfoSnackbar(view: View?, messageText: String?, duration: Int = Snackbar.LENGTH_INDEFINITE) {
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
  fun showSnackbar(view: View, messageText: String, buttonName: String,
                   onClickListener: View.OnClickListener) {
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
  fun showSnackbar(view: View, messageText: String, buttonName: String, duration: Int,
                   onClickListener: View.OnClickListener) {
    snackBar = Snackbar.make(view, messageText, duration).setAction(buttonName, onClickListener)
    snackBar?.show()
  }

  fun dismissSnackBar() {
    snackBar?.dismiss()
  }

  /**
   * Check is current [Activity] connected to some service.
   *
   * @return true if current activity connected to the service, otherwise false.
   */
  protected fun checkServiceBound(isBound: Boolean): Boolean {
    if (!isBound) {
      if (GeneralUtil.isDebugBuild()) {
        LogsUtil.d(tag, "Activity not connected to the service")
      }
      return true
    }
    return false
  }

  protected fun bindService(cls: Class<*>, conn: ServiceConnection) {
    bindService(Intent(this, cls), conn, Context.BIND_AUTO_CREATE)
    LogsUtil.d(tag, "bind to " + cls.simpleName)
  }

  /**
   * Disconnect from a service
   */
  protected fun unbindService(cls: Class<*>, conn: ServiceConnection) {
    unbindService(conn)
    LogsUtil.d(tag, "unbind from " + cls.simpleName)
  }

  /**
   * Register a reply [Messenger] to receive notifications from some service.
   *
   * @param what             A [Message.what]}
   * @param serviceMessenger A service [Messenger]
   * @param replyToMessenger A reply to [Messenger]
   */
  protected fun registerReplyMessenger(what: Int, serviceMessenger: Messenger, replyToMessenger: Messenger) {
    val action = BaseService.Action(replyMessengerName, -1, null)

    val message = Message.obtain(null, what, action)
    message.replyTo = replyToMessenger
    try {
      serviceMessenger.send(message)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Unregister a reply [Messenger] from some service.
   *
   * @param what             A [Message.what]}
   * @param serviceMessenger A service [Messenger]
   * @param replyToMessenger A reply to [Messenger]
   */
  protected fun unregisterReplyMessenger(what: Int, serviceMessenger: Messenger, replyToMessenger: Messenger) {
    val action = BaseService.Action(replyMessengerName, -1, null)

    val message = Message.obtain(null, what, action)
    message.replyTo = replyToMessenger
    try {
      serviceMessenger.send(message)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  protected open fun onNodeStateChanged(nodeInitResult: Node.NodeInitResult) {

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
      accountManager.removeAccountExplicitly(account)
    }
  }

  protected fun logout() {
    lifecycleScope.launch {
      activeAccount?.let { accountEntity ->
        countingIdlingResource.incrementSafely()

        val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
        //remove all info about the given account from the local db
        roomDatabase.accountDao().deleteSuspend(accountEntity)
        removeAccountFromAccountManager(accountEntity)

        //todo-denbond7 Improve this via onDelete = ForeignKey.CASCADE
        roomDatabase.labelDao().deleteByEmailSuspend(accountEntity.email)
        roomDatabase.msgDao().deleteByEmailSuspend(accountEntity.email)
        roomDatabase.attachmentDao().deleteByEmailSuspend(accountEntity.email)
        roomDatabase.accountAliasesDao().deleteByEmailSuspend(accountEntity.email)

        val nonactiveAccounts = roomDatabase.accountDao().getAllNonactiveAccountsSuspend()
        if (nonactiveAccounts.isNotEmpty()) {
          val firstNonactiveAccount = nonactiveAccounts.first()
          roomDatabase.accountDao().updateAccountsSuspend(roomDatabase.accountDao().getAccountsSuspend().map { it.copy(isActive = false) })
          roomDatabase.accountDao().updateAccountSuspend(firstNonactiveAccount.copy(isActive = true))
          EmailSyncService.switchAccount(applicationContext)
          EmailManagerActivity.runEmailManagerActivity(this@BaseActivity)
          finish()
        } else {
          stopService(Intent(applicationContext, EmailSyncService::class.java))
          val intent = Intent(applicationContext, SignInActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          startActivity(intent)
          finish()
        }

        countingIdlingResource.decrementSafely()
      }
    }
  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this, Observer {
      activeAccount = it
      isAccountInfoReceived = true
      onAccountInfoRefreshed(activeAccount)
    })
  }

  private fun registerNodeIdlingResources() {
    Node.getInstance(application).liveData.observe(this, Observer { nodeInitResult ->
      onNodeStateChanged(nodeInitResult)
      nodeIdlingResource.setIdleState(nodeInitResult.isReady)
    })
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

  /**
   * The incoming handler realization. This handler will be used to communicate with a service and other Android
   * components.
   */
  protected class ReplyHandler internal constructor(onServiceCallback: BaseService.OnServiceCallback) : Handler() {
    private val weakRef: WeakReference<BaseService.OnServiceCallback> = WeakReference(onServiceCallback)

    override fun handleMessage(message: Message) {
      if (weakRef.get() != null) {
        when (message.what) {
          BaseService.REPLY_OK -> weakRef.get()?.onReplyReceived(message.arg1, message.arg2, message.obj)

          BaseService.REPLY_ERROR -> {
            var exception: Exception? = null

            if (message.obj is Exception) {
              exception = message.obj as Exception
            }

            weakRef.get()?.onErrorHappened(message.arg1, message.arg2, exception!!)
          }

          BaseService.REPLY_ACTION_PROGRESS -> weakRef.get()?.onProgressReplyReceived(message.arg1, message.arg2, message.obj)

          BaseService.REPLY_ACTION_CANCELED -> weakRef.get()?.onCanceled(message.arg1, message.arg2, message.obj)
        }
      }
    }
  }
}
