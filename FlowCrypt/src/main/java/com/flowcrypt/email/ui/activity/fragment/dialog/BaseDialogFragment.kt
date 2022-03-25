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

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseDialogFragment : DialogFragment() {

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
