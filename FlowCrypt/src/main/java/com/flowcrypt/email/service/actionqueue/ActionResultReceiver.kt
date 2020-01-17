/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue

import android.app.IntentService
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.util.GeneralUtil

/**
 * A custom implementation of [ResultReceiver] for a communication with [ActionQueueIntentService]
 *
 * @author Denis Bondarenko
 * Date: 31.01.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */

class ActionResultReceiver
/**
 * Create a new ResultReceive to receive results.  Your
 * [.onReceiveResult] method will be called from the thread running
 * <var>handler</var> if given, or from an arbitrary thread if null.
 *
 * @param handler It will receive results from the [IntentService]
 */
(handler: Handler) : ResultReceiver(handler) {

  private var resultReceiverCallBack: ResultReceiverCallBack? = null

  override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
    if (resultReceiverCallBack != null) {
      val action = resultData.getParcelable<Action>(EXTRA_KEY_ACTION)
      when (resultCode) {
        RESULT_CODE_OK -> resultReceiverCallBack!!.onSuccess(resultData.getParcelable(EXTRA_KEY_ACTION))

        RESULT_CODE_ERROR -> {
          val e = resultData.getSerializable(EXTRA_KEY_EXCEPTION) as java.lang.Exception
          resultReceiverCallBack!!.onError(e, action)
        }
      }
    }
  }

  fun setResultReceiverCallBack(resultReceiverCallBack: ResultReceiverCallBack) {
    this.resultReceiverCallBack = resultReceiverCallBack
  }

  /**
   * A callback for handling results from the [ActionQueueIntentService]
   */
  interface ResultReceiverCallBack {
    /**
     * This method will be called if [Action] success.
     *
     * @param action An input [Action]
     */
    fun onSuccess(action: Action?)

    /**
     * This method will be called if an error will happen when we try to run some [Action].
     *
     * @param exception A happened exception
     * @param action    An input [Action]
     */
    fun onError(exception: Exception, action: Action?)
  }

  companion object {
    const val RESULT_CODE_OK = 1
    const val RESULT_CODE_ERROR = 0

    private val EXTRA_KEY_ACTION = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACTION",
        ActionResultReceiver::class.java)
    private val EXTRA_KEY_EXCEPTION = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_EXCEPTION",
        ActionResultReceiver::class.java)

    @JvmStatic
    fun generateSuccessBundle(action: Action): Bundle {
      val bundle = Bundle()
      bundle.putParcelable(EXTRA_KEY_ACTION, action)
      return bundle
    }

    @JvmStatic
    fun generateErrorBundle(action: Action, e: Exception): Bundle {
      val bundle = generateSuccessBundle(action)
      bundle.putSerializable(EXTRA_KEY_EXCEPTION, e)
      return bundle
    }
  }
}
