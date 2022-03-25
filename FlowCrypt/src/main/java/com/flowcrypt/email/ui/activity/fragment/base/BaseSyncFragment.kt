/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.flowcrypt.email.R

/**
 * The base fragment which must used when we will work with an email provider.
 *
 * @author DenBond7
 * Date: 04.05.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseSyncFragment : BaseFragment() {

  protected var progressView: View? = null
  protected var statusView: View? = null
  protected var tVStatusMsg: TextView? = null

  /**
   * Get a content view which contains a UI.
   *
   * @return <tt>View</tt> Return a progress view.
   */
  abstract val contentView: View?

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    progressView = view.findViewById(R.id.viewIdProgressView)
    statusView = view.findViewById(R.id.viewIdStatusView)
    tVStatusMsg = view.findViewById(R.id.tVStatusMsg)
    if (progressView == null || statusView == null || tVStatusMsg == null) {
      throw IllegalArgumentException("The layout file of this fragment not contains " + "some needed views")
    }
  }
}
