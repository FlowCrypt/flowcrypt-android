/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.util.IdlingCountListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * The base dialog fragment.
 *
 * @author Denys Bondarenko
 */
abstract class BaseDialogFragment : DialogFragment(), IdlingCountListener {

  private var idlingCount: AtomicInteger = AtomicInteger(0)

  override fun incrementIdlingCount() {
    IdlingCountListener.handleIncrement(idlingCount, this.javaClass)
  }

  override fun decrementIdlingCount() {
    IdlingCountListener.handleDecrement(idlingCount, this.javaClass)
  }

  override fun onDestroy() {
    super.onDestroy()
    IdlingCountListener.printIdlingStats(idlingCount, this.javaClass)
  }

  protected fun modifyLinkMovementMethod(hasHtml: Boolean, useLinkify: Boolean) {
    if (hasHtml) {
      (dialog?.findViewById<View>(android.R.id.message) as? TextView)?.apply {
        movementMethod = LinkMovementMethod.getInstance()
      }
    } else if (useLinkify) {
      (dialog?.findViewById<View>(android.R.id.message) as? TextView)?.apply {
        autoLinkMask = Linkify.ALL
        movementMethod = LinkMovementMethod.getInstance()
      }
    }
  }
}
