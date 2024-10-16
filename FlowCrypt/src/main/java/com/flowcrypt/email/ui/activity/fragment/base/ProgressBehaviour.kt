/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone

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

  fun showProgress(
    progressMsg: String? = null,
    useHorizontalProgressBar: Boolean = false,
    progress: Int = 0
  ) {
    contentView?.gone()
    goneStatusView()

    val tVProgressMsg = progressView?.findViewById<TextView>(R.id.tVProgressMsg)
    tVProgressMsg?.text = progressMsg

    val progressBar = progressView?.findViewById<ProgressBar>(R.id.progressBar)
    val progressBarHorizontal = progressView?.findViewById<ProgressBar>(R.id.progressBarHorizontal)

    progressBar?.visibleOrGone(!useHorizontalProgressBar)
    progressBarHorizontal?.visibleOrGone(useHorizontalProgressBar)

    if (useHorizontalProgressBar) {
      if (progress > 0) {
        progressBarHorizontal?.isIndeterminate = false
        progressBarHorizontal?.progress = progress
      } else {
        progressBarHorizontal?.isIndeterminate = true
        progressBarHorizontal?.progress = 0
      }
    }

    progressView?.visible()
  }

  fun showContent() {
    goneStatusView()
    goneProgressView()
    contentView?.visible()
  }

  fun showStatus(
    msg: String? = null,
    resourcesId: Int = R.drawable.ic_warning_red_24dp,
    action: (() -> Unit)? = null
  ) {
    goneProgressView()
    contentView?.gone()

    val tvStatusMsg = statusView?.findViewById<TextView>(R.id.tVStatusMsg)
    tvStatusMsg?.text = msg

    if (resourcesId > 0) {
      val iVStatusImg = statusView?.findViewById<ImageView>(R.id.iVStatusImg)
      iVStatusImg?.setImageResource(resourcesId)
    }

    if (action != null) {
      statusView?.findViewById<View>(R.id.buttonRetry)?.apply {
        visible()
        setOnClickListener {
          action.invoke()
        }
      }
    }

    statusView?.visible()
  }

  fun goneStatusView() {
    statusView?.gone()
    statusView?.findViewById<TextView>(R.id.tVStatusMsg)?.text = null
    statusView?.findViewById<ImageView>(R.id.iVStatusImg)?.setImageDrawable(null)
  }

  fun goneProgressView() {
    progressView?.gone()
    progressView?.findViewById<TextView>(R.id.tVProgressMsg)?.text = null
  }
}
