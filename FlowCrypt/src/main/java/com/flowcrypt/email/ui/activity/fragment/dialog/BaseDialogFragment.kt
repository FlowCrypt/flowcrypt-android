/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseDialogFragment : DialogFragment() {
  protected var hasHtml: Boolean = false
  protected var useLinkify: Boolean = false

  val baseActivity: BaseActivity?
    get() = activity as? BaseActivity

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    hasHtml = arguments?.getBoolean(KEY_INFO_HAS_HTML, false) ?: false
    useLinkify = arguments?.getBoolean(KEY_INFO_USE_LINKIFY, false) ?: false
  }

  override fun onStart() {
    super.onStart()
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

  companion object {
    val KEY_INFO_HAS_HTML =
      GeneralUtil.generateUniqueExtraKey("KEY_INFO_HAS_HTML", BaseDialogFragment::class.java)
    val KEY_INFO_USE_LINKIFY =
      GeneralUtil.generateUniqueExtraKey("KEY_INFO_USE_LINKIFY", BaseDialogFragment::class.java)
  }
}
