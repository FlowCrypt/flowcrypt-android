/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.app.Service
import android.os.Messenger

import com.flowcrypt.email.api.email.sync.SyncErrorTypes

/**
 * The base [Service] class for a between threads communication.
 *
 * @author Denis Bondarenko
 * Date: 16.02.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseService : Service() {
  interface OnServiceCallback {
    /**
     * In this method we can handle response after run some action via [BaseService]
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     * over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?)

    /**
     * In this method we can handle a progress state after run some action via [BaseService]
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     * over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?)

    /**
     * In this method we can handle en error after run some action via [BaseService]
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     * over all project.
     * @param errorType   The [SyncErrorTypes].
     * @param e           The exception which occurred.
     */
    fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception)
  }

  /**
   * This class can be used to create a new action for [BaseService]
   */
  class Action
  /**
   * The constructor.
   *
   * @param ownerKey    The name of reply to [Messenger]
   * @param requestCode The unique request code which identify some action
   * @param object      The object which will be passed to [BaseService].
   * @param resetConnection The reset connection status.
   */
  (val ownerKey: String, val requestCode: Int, val `object`: Any?, val resetConnection: Boolean = false) {

    override fun toString(): String {
      return "Action{" +
          "ownerKey='" + ownerKey + '\''.toString() +
          ", requestCode=" + requestCode +
          ", object=" + `object` +
          ", resetConnection=" + resetConnection +
          '}'.toString()
    }
  }

  companion object {
    const val REPLY_OK = 0
    const val REPLY_ERROR = 1
    const val REPLY_ACTION_PROGRESS = 2
  }
}
