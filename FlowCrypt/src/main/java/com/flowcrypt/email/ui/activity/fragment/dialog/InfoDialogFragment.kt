/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.setNavigationResult
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import java.nio.charset.StandardCharsets

/**
 * This class can be used to show an info dialog to the user.
 *
 * @author Denis Bondarenko
 * Date: 24.07.2017
 * Time: 17:34
 * E-mail: DenBond7@gmail.com
 */
class InfoDialogFragment : BaseDialogFragment() {
  private val args by navArgs<InfoDialogFragmentArgs>()

  private var dialogTitle: String? = null
  private var dialogMsg: String? = null
  private var buttonTitle: String? = null
  private var isPopBackStack: Boolean = false
  var onInfoDialogButtonClickListener: OnInfoDialogButtonClickListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnInfoDialogButtonClickListener) {
      onInfoDialogButtonClickListener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    hasHtml = arguments?.getBoolean(KEY_INFO_HAS_HTML, args.hasHtml) ?: false
    useLinkify = arguments?.getBoolean(KEY_INFO_USE_LINKIFY, args.useLinkify) ?: false
    dialogTitle = arguments?.getString(
      KEY_INFO_DIALOG_TITLE, args.dialogTitle ?: getString(R.string.info)
    )
    dialogMsg = arguments?.getString(KEY_INFO_DIALOG_MESSAGE, args.dialogMsg)
    buttonTitle = arguments?.getString(
      KEY_INFO_BUTTON_TITLE, args.buttonTitle ?: getString(android.R.string.ok)
    )
    isPopBackStack = arguments?.getBoolean(
      KEY_INFO_IS_POP_BACK_STACK, args.useNavigationUp
    ) ?: false
    isCancelable = arguments?.getBoolean(KEY_INFO_IS_CANCELABLE, args.isCancelable) ?: false
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(requireActivity()).apply {
      setTitle(dialogTitle)
      if (!args.useWebViewToRender) {
        setMessage(if (hasHtml) UIUtil.getHtmlSpannedFromText(dialogMsg) else dialogMsg)
      }
      setPositiveButton(buttonTitle) { _, _ ->
        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
        onInfoDialogButtonClickListener?.onInfoDialogButtonClick(targetRequestCode)
        setNavigationResult(KEY_RESULT, RESULT_OK)

        if (isPopBackStack) {
          val fragmentManager = requireActivity().supportFragmentManager
          fragmentManager.popBackStackImmediate()
          navController?.navigateUp()
        }
      }
    }

    val dialog = builder.create()
    if (args.useWebViewToRender) {
      val webView = WebView(requireContext())
      webView.id = R.id.webView
      dialogMsg?.let {
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

  interface OnInfoDialogButtonClickListener {
    fun onInfoDialogButtonClick(requestCode: Int)
  }

  companion object {
    val KEY_RESULT =
      GeneralUtil.generateUniqueExtraKey("KEY_RESULT", InfoDialogFragment::class.java)
    const val RESULT_OK = -1

    private val KEY_INFO_DIALOG_TITLE =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_INFO_DIALOG_TITLE",
        InfoDialogFragment::class.java
      )
    private val KEY_INFO_DIALOG_MESSAGE =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_INFO_DIALOG_MESSAGE",
        InfoDialogFragment::class.java
      )
    private val KEY_INFO_BUTTON_TITLE =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_INFO_BUTTON_TITLE",
        InfoDialogFragment::class.java
      )
    private val KEY_INFO_IS_POP_BACK_STACK =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_INFO_IS_POP_BACK_STACK",
        InfoDialogFragment::class.java
      )
    private val KEY_INFO_IS_CANCELABLE =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_INFO_IS_CANCELABLE",
        InfoDialogFragment::class.java
      )

    private val KEY_USE_WEB_VIEW_TO_RENDER =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_USE_WEB_VIEW_TO_RENDER",
        InfoDialogFragment::class.java
      )

    fun newInstance(
      dialogTitle: String? = null,
      dialogMsg: String? = null,
      buttonTitle: String? = null,
      isPopBackStack: Boolean = false,
      isCancelable: Boolean = false,
      hasHtml: Boolean = false,
      useWebViewToRender: Boolean = false,
      useLinkify: Boolean = false
    ): InfoDialogFragment {
      val dialogFragment = InfoDialogFragment()

      val args = Bundle()
      args.putString(KEY_INFO_DIALOG_TITLE, dialogTitle)
      args.putString(KEY_INFO_DIALOG_MESSAGE, dialogMsg)
      args.putString(KEY_INFO_BUTTON_TITLE, buttonTitle)
      args.putBoolean(KEY_INFO_IS_POP_BACK_STACK, isPopBackStack)
      args.putBoolean(KEY_INFO_IS_CANCELABLE, isCancelable)
      args.putBoolean(KEY_INFO_HAS_HTML, hasHtml)
      args.putBoolean(KEY_USE_WEB_VIEW_TO_RENDER, useWebViewToRender)
      args.putBoolean(KEY_INFO_USE_LINKIFY, useLinkify)
      dialogFragment.arguments = args

      return dialogFragment
    }
  }
}
