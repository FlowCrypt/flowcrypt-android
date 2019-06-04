/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity
import com.flowcrypt.email.util.UIUtil

/**
 * The base fragment which must used when we will work with an email provider.
 *
 * @author DenBond7
 * Date: 04.05.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseSyncFragment : BaseFragment() {

  @JvmField
  protected var progressView: View? = null
  @JvmField
  protected var statusView: View? = null
  @JvmField
  protected var textViewStatusInfo: TextView? = null

  /**
   * Get a content view which contains a UI.
   *
   * @return <tt>View</tt> Return a progress view.
   */
  abstract val contentView: View?

  /**
   * Check is we connected to the sync service.
   *
   * @return true if we connected, otherwise false.
   */
  val isSyncServiceConnected: Boolean
    get() {
      val baseSyncActivity = activity as BaseSyncActivity?
      return baseSyncActivity?.isSyncServiceBound ?: throw NullPointerException("BaseSyncActivity is null!")
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    progressView = view.findViewById(R.id.viewIdProgressView)
    statusView = view.findViewById(R.id.viewIdStatusView)
    textViewStatusInfo = view.findViewById(R.id.viewIdTextViewStatusInfo)
    if (progressView == null || statusView == null || textViewStatusInfo == null) {
      throw IllegalArgumentException("The layout file of this fragment not contains " + "some needed views")
    }
  }

  /**
   * Handle an error from the sync service.
   *
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param errorType   The [SyncErrorTypes]
   * @param e           The exception which happened.
   */
  open fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception?) {
    contentView?.visibility = View.GONE

    when (errorType) {
      SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST -> textViewStatusInfo!!.setText(R.string.there_was_syncing_problem)

      else -> if (e != null && !TextUtils.isEmpty(e.message)) {
        textViewStatusInfo!!.text = e.message
      } else {
        textViewStatusInfo!!.setText(R.string.unknown_error)
      }
    }

    UIUtil.exchangeViewVisibility(context, false, progressView!!, statusView!!)
    if (snackBar != null) {
      snackBar!!.dismiss()
    }
  }
}
