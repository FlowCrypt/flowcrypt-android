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
 * @author Denis Bondarenko
 *         Date: 2/18/20
 *         Time: 10:12 AM
 *         E-mail: DenBond7@gmail.com
 */
interface ProgressBehaviour {
  val progressView: View?
  val contentView: View?
  val statusView: View?

  fun showProgress(progressMsg: String? = null) {
    contentView?.visibility = View.GONE
    statusView?.visibility = View.GONE

    val tVProgressMsg = progressView?.findViewById<TextView>(R.id.tVProgressMsg)
    tVProgressMsg?.text = progressMsg

    progressView?.visibility = View.VISIBLE
  }

  fun showContent() {
    statusView?.visibility = View.GONE
    progressView?.visibility = View.GONE
    contentView?.visibility = View.VISIBLE
  }

  fun showStatus(msg: String? = null, resourcesId: Int = 0) {
    progressView?.visibility = View.GONE
    contentView?.visibility = View.GONE

    val tvStatusMsg = statusView?.findViewById<TextView>(R.id.tVStatusMsg)
    tvStatusMsg?.text = msg

    if (resourcesId > 0) {
      val iVStatusImg = statusView?.findViewById<ImageView>(R.id.iVStatusImg)
      iVStatusImg?.setImageResource(resourcesId)
    }

    statusView?.visibility = View.VISIBLE
  }
}