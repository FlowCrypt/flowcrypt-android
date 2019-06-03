/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.service.BaseService
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.idling.NodeIdlingResource
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
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
  protected val tag: String = javaClass.simpleName
  @get:VisibleForTesting
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
      Node.getInstance(application).liveData.value!!
    }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {

  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    registerNodeIdlingResources()
    LogsUtil.d(tag, "onCreate")
    if (contentViewResourceId != 0) {
      setContentView(contentViewResourceId)
      initScreenViews()
    }
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
    LogsUtil.d(tag, "onDestroy")
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> if (isDisplayHomeAsUpEnabled) {
        finish()
        true
      } else
        super.onOptionsItemSelected(item)

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
  @JvmOverloads
  fun showInfoSnackbar(view: View, messageText: String?, duration: Int = Snackbar.LENGTH_INDEFINITE) {
    snackBar = Snackbar.make(view, messageText ?: "", duration).setAction(android.R.string.ok) { }
    snackBar!!.show()
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
    snackBar!!.show()
  }

  fun dismissSnackBar() {
    if (snackBar != null) {
      snackBar!!.dismiss()
    }
  }

  fun handleLoaderResult(loader: Loader<*>, loaderResult: LoaderResult?) {
    if (loaderResult != null) {
      when {
        loaderResult.result != null -> onSuccess(loader.id, loaderResult.result)
        loaderResult.exception != null -> onError(loader.id, loaderResult.exception)
        else -> showInfoSnackbar(rootView, getString(R.string.unknown_error))
      }
    } else {
      showInfoSnackbar(rootView, getString(R.string.unknown_error))
    }
  }

  open fun onError(loaderId: Int, e: Exception?) {

  }

  open fun onSuccess(loaderId: Int, result: Any?) {

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

  protected open fun onNodeStateChanged(isReady: Boolean) {

  }

  private fun registerNodeIdlingResources() {
    Node.getInstance(application).liveData.observe(this, Observer { aBoolean ->
      nodeIdlingResource.setIdleState(aBoolean!!)
      onNodeStateChanged(aBoolean)
    })
  }

  private fun initScreenViews() {
    appBarLayout = findViewById(R.id.appBarLayout)
    setupToolbar()
  }

  private fun setupToolbar() {
    toolbar = findViewById(R.id.toolbar)
    if (toolbar != null) {
      setSupportActionBar(toolbar)
    }

    if (supportActionBar != null) {
      supportActionBar!!.setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled)
    }
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

          BaseService.REPLY_ACTION_PROGRESS -> weakRef.get()?.onProgressReplyReceived(message.arg1, message.arg2,
              message.obj)
        }
      }
    }
  }
}
