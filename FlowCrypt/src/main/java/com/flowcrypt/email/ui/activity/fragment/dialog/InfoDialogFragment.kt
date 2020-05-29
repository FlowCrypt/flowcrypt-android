/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This class can be used to show an info dialog to the user.
 *
 * @author Denis Bondarenko
 * Date: 24.07.2017
 * Time: 17:34
 * E-mail: DenBond7@gmail.com
 */
class InfoDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

  private var dialogTitle: String? = null
  private var dialogMsg: String? = null
  private var buttonTitle: String? = null
  private var isPopBackStack: Boolean = false
  private var hasHtml: Boolean = false
  var onInfoDialogButtonClickListener: OnInfoDialogButtonClickListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnInfoDialogButtonClickListener) {
      onInfoDialogButtonClickListener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dialogTitle = arguments?.getString(KEY_INFO_DIALOG_TITLE, getString(R.string.info))
    dialogMsg = arguments?.getString(KEY_INFO_DIALOG_MESSAGE)
    buttonTitle = arguments?.getString(KEY_INFO_BUTTON_TITLE, getString(android.R.string.ok))
    isPopBackStack = arguments?.getBoolean(KEY_INFO_IS_POP_BACK_STACK, false) ?: false
    hasHtml = arguments?.getBoolean(KEY_INFO_HAS_HTML, false) ?: false
    isCancelable = arguments?.getBoolean(KEY_INFO_IS_CANCELABLE, false) ?: false
  }

  override fun onStart() {
    super.onStart()
    if (hasHtml) {
      (dialog?.findViewById<View>(android.R.id.message) as? TextView)?.movementMethod = LinkMovementMethod.getInstance()
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(requireActivity())
    dialog.setTitle(dialogTitle)
    dialog.setMessage(if (hasHtml) UIUtil.getHtmlSpannedFromText(dialogMsg) else dialogMsg)
    dialog.setPositiveButton(buttonTitle, this)
    return dialog.create()
  }

  override fun onClick(dialog: DialogInterface, which: Int) {
    when (which) {
      Dialog.BUTTON_POSITIVE -> {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
        onInfoDialogButtonClickListener?.onInfoDialogButtonClick(targetRequestCode)

        if (isPopBackStack) {
          val fragmentManager = requireActivity().supportFragmentManager
          fragmentManager.popBackStackImmediate()
        }
      }
    }
  }

  interface OnInfoDialogButtonClickListener {
    fun onInfoDialogButtonClick(requestCode: Int)
  }

  companion object {
    const val RESULT_OK = -1

    private val KEY_INFO_DIALOG_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_DIALOG_TITLE", InfoDialogFragment::class.java)
    private val KEY_INFO_DIALOG_MESSAGE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_DIALOG_MESSAGE", InfoDialogFragment::class.java)
    private val KEY_INFO_BUTTON_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_BUTTON_TITLE", InfoDialogFragment::class.java)
    private val KEY_INFO_IS_POP_BACK_STACK =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_IS_POP_BACK_STACK", InfoDialogFragment::class.java)
    private val KEY_INFO_IS_CANCELABLE =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_IS_CANCELABLE", InfoDialogFragment::class.java)
    private val KEY_INFO_HAS_HTML =
        GeneralUtil.generateUniqueExtraKey("KEY_INFO_HAS_HTML", InfoDialogFragment::class.java)

    fun newInstance(dialogTitle: String? = null, dialogMsg: String? = null,
                    buttonTitle: String? = null, isPopBackStack: Boolean = false,
                    isCancelable: Boolean = false, hasHtml: Boolean = false): InfoDialogFragment {
      val infoDialogFragment = InfoDialogFragment()

      val args = prepareArgs(dialogTitle, dialogMsg, buttonTitle, isPopBackStack, isCancelable, hasHtml)
      infoDialogFragment.arguments = args

      return infoDialogFragment
    }

    private fun prepareArgs(dialogTitle: String?, dialogMsg: String?, buttonTitle: String?,
                            isPopBackStack: Boolean, isCancelable: Boolean, hasHtml: Boolean): Bundle {
      val args = Bundle()
      args.putString(KEY_INFO_DIALOG_TITLE, dialogTitle)
      args.putString(KEY_INFO_DIALOG_MESSAGE, dialogMsg)
      args.putString(KEY_INFO_BUTTON_TITLE, buttonTitle)
      args.putBoolean(KEY_INFO_IS_POP_BACK_STACK, isPopBackStack)
      args.putBoolean(KEY_INFO_IS_CANCELABLE, isCancelable)
      args.putBoolean(KEY_INFO_HAS_HTML, hasHtml)
      return args
    }
  }
}
