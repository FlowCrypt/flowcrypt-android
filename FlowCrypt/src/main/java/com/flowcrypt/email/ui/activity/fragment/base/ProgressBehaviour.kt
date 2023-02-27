/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.flowcrypt.email.R

/**
 * This interface describes a situation when we should to show some progress while some operation
 * is running. Here we should to have 'progressView', 'contentView' and 'statusView' to display
 * errors.
 *
 * @author Denys Bondarenko
 */
interface ProgressBehaviour {
  val progressView: View?
  val contentView: View?
  val statusView: View?

  fun showProgress(progressMsg: String? = null) {
    contentView?.visibility = View.GONE
    goneStatusView()

    val tVProgressMsg = progressView?.findViewById<TextView>(R.id.tVProgressMsg)
    tVProgressMsg?.text = progressMsg

    progressView?.visibility = View.VISIBLE
  }

  fun showContent() {
    goneStatusView()
    goneProgressView()
    contentView?.visibility = View.VISIBLE
  }

  fun showStatus(msg: String? = null, resourcesId: Int = 0) {
    goneProgressView()
    contentView?.visibility = View.GONE

    val tvStatusMsg = statusView?.findViewById<TextView>(R.id.tVStatusMsg)
    tvStatusMsg?.text = msg

    if (resourcesId > 0) {
      val iVStatusImg = statusView?.findViewById<ImageView>(R.id.iVStatusImg)
      iVStatusImg?.setImageResource(resourcesId)
    }

    statusView?.visibility = View.VISIBLE
  }

  fun goneStatusView() {
    statusView?.visibility = View.GONE
    statusView?.findViewById<TextView>(R.id.tVStatusMsg)?.text = null
    statusView?.findViewById<ImageView>(R.id.iVStatusImg)?.setImageDrawable(null)
  }

  fun goneProgressView() {
    progressView?.visibility = View.GONE
    progressView?.findViewById<TextView>(R.id.tVProgressMsg)?.text = null
  }
}
