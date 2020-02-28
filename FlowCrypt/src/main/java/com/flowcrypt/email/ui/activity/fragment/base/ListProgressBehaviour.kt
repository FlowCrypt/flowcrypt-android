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
 * This interface helps to show a list of some data. It extends [ProgressBehaviour] with one more
 * common view - 'emptyView'.
 *
 * @author Denis Bondarenko
 *         Date: 2/18/20
 *         Time: 10:16 AM
 *         E-mail: DenBond7@gmail.com
 */
interface ListProgressBehaviour : ProgressBehaviour {
  val emptyView: View?

  fun showEmptyView(msg: String? = null, resourcesId: Int = 0) {
    contentView?.visibility = View.GONE
    statusView?.visibility = View.GONE
    progressView?.visibility = View.GONE

    val tVEmpty = emptyView?.findViewById<TextView>(R.id.tVEmpty)
    tVEmpty?.text = msg

    if (resourcesId > 0) {
      val iVEmptyImg = emptyView?.findViewById<ImageView>(R.id.iVEmptyImg)
      iVEmptyImg?.setImageResource(resourcesId)
    }

    emptyView?.visibility = View.VISIBLE
  }
}