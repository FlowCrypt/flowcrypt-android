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
import com.flowcrypt.email.util.LogsUtil
import java.util.concurrent.atomic.AtomicInteger

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseDialogFragment : DialogFragment(), IdlingCountListener {

  private var idlingCount: AtomicInteger = AtomicInteger(0)

  override fun incrementIdlingCount() {
    idlingCount.incrementAndGet()
    LogsUtil.d(
      this.javaClass.simpleName,
      this.javaClass.simpleName + ":>>>> = " + idlingCount + "|" + idlingCount.hashCode()
    )
  }

  override fun decrementIdlingCount() {
    idlingCount.decrementAndGet()
    LogsUtil.d(
      this.javaClass.simpleName,
      this.javaClass.simpleName + ":<<<< = " + idlingCount + "|" + idlingCount.hashCode()
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(
      this.javaClass.simpleName,
      this.javaClass.simpleName + ":idlingCount = " + idlingCount + "|" + idlingCount.hashCode()
    )
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
