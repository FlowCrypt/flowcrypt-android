/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.util.GeneralUtil
import java.nio.charset.StandardCharsets

/**
 * This class can be used to show an info dialog to the user using [WebView].
 *
 * @author Denis Bondarenko
 * Date: 05.02.2018
 * Time: 15:56
 * E-mail: DenBond7@gmail.com
 */
class WebViewInfoDialogFragment : DialogFragment(), View.OnClickListener {

  protected var dialogTitle: String? = null
  protected var dialogMsg: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments

    if (args != null) {
      dialogTitle = args.getString(KEY_INFO_DIALOG_TITLE, getString(R.string.info))
      dialogMsg = args.getString(KEY_INFO_DIALOG_MESSAGE)
      isCancelable = args.getBoolean(KEY_INFO_IS_CANCELABLE, true)
    }
  }

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(requireContext())
    dialog.setTitle(dialogTitle)

    val rootView = LayoutInflater.from(context).inflate(R.layout.fragment_info_in_webview, null)
    rootView.findViewById<View>(R.id.buttonOk).setOnClickListener(this)

    val webView = rootView.findViewById<WebView>(R.id.webView)
    webView.loadDataWithBaseURL(null, dialogMsg, "text/html", StandardCharsets.UTF_8.displayName(), null)
    dialog.setView(rootView)
    return dialog.create()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonOk -> dismiss()
    }
  }

  companion object {
    private val KEY_INFO_DIALOG_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_DIALOG_TITLE", WebViewInfoDialogFragment::class.java)
    private val KEY_INFO_DIALOG_MESSAGE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_DIALOG_MESSAGE", WebViewInfoDialogFragment::class.java)
    private val KEY_INFO_IS_CANCELABLE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_IS_CANCELABLE", WebViewInfoDialogFragment::class.java)

    fun newInstance(dialogTitle: String, dialogMsg: String, isCancelable: Boolean = true): WebViewInfoDialogFragment {
      val infoDialogFragment = WebViewInfoDialogFragment()

      val args = prepareArgs(dialogTitle, dialogMsg, isCancelable)
      infoDialogFragment.arguments = args

      return infoDialogFragment
    }

    private fun prepareArgs(dialogTitle: String, dialogMsg: String, isCancelable: Boolean): Bundle {
      val args = Bundle()
      args.putString(KEY_INFO_DIALOG_TITLE, dialogTitle)
      args.putString(KEY_INFO_DIALOG_MESSAGE, dialogMsg)
      args.putBoolean(KEY_INFO_IS_CANCELABLE, isCancelable)
      return args
    }
  }
}
