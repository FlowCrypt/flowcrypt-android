/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.text.TextUtils
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import java.lang.ref.WeakReference

/**
 * This service will be used to do checking clipboard to find a valid key while the
 * service running.
 *
 * @author Denys Bondarenko
 */
class CheckClipboardToFindKeyService : Service(), ClipboardManager.OnPrimaryClipChangedListener {

  @Volatile
  private lateinit var serviceWorkerLooper: Looper

  @Volatile
  private lateinit var serviceWorkerHandler: ServiceWorkerHandler

  var keyImportModel: KeyImportModel? = null
    private set
  private val localBinder: IBinder
  private lateinit var clipboardManager: ClipboardManager
  private val replyMessenger: Messenger
  var isPrivateKeyMode: Boolean = false

  init {
    this.localBinder = LocalBinder()
    this.replyMessenger = Messenger(ReplyHandler(this))
  }

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")

    clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.addPrimaryClipChangedListener(this)

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()

    serviceWorkerLooper = handlerThread.looper
    serviceWorkerHandler = ServiceWorkerHandler(serviceWorkerLooper)

    checkClipboard()
  }

  override fun onBind(intent: Intent): IBinder {
    LogsUtil.d(TAG, "onBind:$intent")
    return localBinder
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")

    serviceWorkerLooper.quit()
    clipboardManager.removePrimaryClipChangedListener(this)
  }

  override fun onPrimaryClipChanged() {
    checkClipboard()
  }

  private fun checkClipboard() {
    keyImportModel = null
    if (clipboardManager.hasPrimaryClip()) {
      val item = clipboardManager.primaryClip!!.getItemAt(0)
      val privateKeyFromClipboard = item.text
      if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
        checkClipboardText(privateKeyFromClipboard.toString())
      }
    }
  }

  private fun checkClipboardText(clipboardText: String) {
    val message = serviceWorkerHandler.obtainMessage()
    message.what = ServiceWorkerHandler.MESSAGE_WHAT
    message.obj = clipboardText
    message.replyTo = replyMessenger
    serviceWorkerHandler.removeMessages(ServiceWorkerHandler.MESSAGE_WHAT)
    serviceWorkerHandler.sendMessage(message)
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with current
   * service and the worker thread.
   */
  private class ReplyHandler(checkClipboardToFindKeyService: CheckClipboardToFindKeyService) :
    Handler(Looper.getMainLooper()) {
    private val weakRef: WeakReference<CheckClipboardToFindKeyService> =
      WeakReference(checkClipboardToFindKeyService)

    override fun handleMessage(message: Message) {
      when (message.what) {
        MESSAGE_WHAT -> if (weakRef.get() != null) {
          val checkClipboardToFindKeyService = weakRef.get()

          val key = message.obj as String

          checkClipboardToFindKeyService?.keyImportModel = KeyImportModel(
            null, key,
            weakRef.get()!!.isPrivateKeyMode, KeyImportDetails.SourceType.CLIPBOARD
          )
          LogsUtil.d(TAG, "Found a valid private key in clipboard")
        }
      }
    }

    companion object {
      internal const val MESSAGE_WHAT = 1
    }
  }

  /**
   * This handler will be used by the instance of [HandlerThread] to receive message from
   * the UI thread.
   */
  private class ServiceWorkerHandler(looper: Looper) : Handler(looper) {

    override fun handleMessage(msg: Message) {
      when (msg.what) {
        MESSAGE_WHAT -> {
          val clipboardText = msg.obj as String
          try {
            val pgpKeyDetails = PgpKey.parseKeys(source = clipboardText).pgpKeyDetailsList
            if (!CollectionUtils.isEmpty(pgpKeyDetails)) {
              sendReply(msg)
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }

        }
      }
    }

    private fun sendReply(msg: Message) {
      try {
        val messenger = msg.replyTo
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_WHAT, msg.obj))
      } catch (e: RemoteException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }

    }

    companion object {
      internal const val MESSAGE_WHAT = 1
    }
  }

  /**
   * The local binder realization.
   */
  inner class LocalBinder : Binder() {
    val service: CheckClipboardToFindKeyService
      get() = this@CheckClipboardToFindKeyService
  }

  companion object {
    val TAG = CheckClipboardToFindKeyService::class.java.simpleName
  }
}
