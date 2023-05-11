/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.android.webkit.setupDayNight
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import java.nio.charset.StandardCharsets


/**
 * This class can be used to show an info dialog to the user.
 *
 * @author Denys Bondarenko
 */
class InfoDialogFragment : BaseDialogFragment() {
  private val args by navArgs<InfoDialogFragmentArgs>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = args.isCancelable
  }

  override fun onStart() {
    super.onStart()
    modifyLinkMovementMethod(args.hasHtml, args.useLinkify)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(requireActivity()).apply {
      setTitle(args.dialogTitle)
      if (!args.useWebViewToRender) {
        setMessage(if (args.hasHtml) UIUtil.getHtmlSpannedFromText(args.dialogMsg) else args.dialogMsg)
      }
      setPositiveButton(args.buttonTitle) { _, _ ->
        navController?.navigateUp()
        args.requestKey?.let { requestKey ->
          setFragmentResult(
            requestKey,
            bundleOf(KEY_REQUEST_CODE to args.requestCode)
          )
        }
      }
    }

    val dialog = builder.create()
    if (args.useWebViewToRender) {
      val webView = WebView(requireContext())
      webView.id = R.id.webView
      webView.setupDayNight()

      args.dialogMsg?.let {
        webView.loadDataWithBaseURL(
          null,
          it,
          "text/html",
          StandardCharsets.UTF_8.displayName(),
          null
        )
      }
      val padding = requireContext().resources.getDimensionPixelSize(R.dimen.default_margin_content)
      dialog.setView(webView, padding, padding, padding, padding)
    }
    return dialog
  }

  override fun onResume() {
    super.onResume()
    if (!args.useWebViewToRender) {
      val textView = dialog?.window?.decorView?.findViewById(android.R.id.message) as? TextView
      textView?.setTextIsSelectable(true)
    }
  }

  companion object {
    val KEY_REQUEST_CODE = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_CODE", InfoDialogFragment::class.java
    )
  }
}
