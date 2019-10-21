/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.util.GeneralUtil

/**
 * This dialog can be used if we need to show a simple info dialog which has two buttons (negative and positive).
 *
 * @author Denis Bondarenko
 * Date: 28.08.2018
 * Time: 15:28
 * E-mail: DenBond7@gmail.com
 */
class TwoWayDialogFragment : DialogFragment() {

  private var dialogTitle: String? = null
  private var dialogMsg: String? = null
  private var positiveBtnTitle: String? = null
  private var negativeBtnTitle: String? = null
  private var listener: OnTwoWayDialogListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnTwoWayDialogListener) {
      this.listener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments

    if (args != null) {
      dialogTitle = args.getString(KEY_DIALOG_TITLE)
      dialogMsg = args.getString(KEY_DIALOG_MESSAGE)
      positiveBtnTitle = args.getString(KEY_POSITIVE_BUTTON_TITLE, getString(R.string.yes))
      negativeBtnTitle = args.getString(KEY_NEGATIVE_BUTTON_TITLE, getString(R.string.no))
      isCancelable = args.getBoolean(KEY_IS_CANCELABLE, false)
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialogBuilder = AlertDialog.Builder(context!!)

    if (dialogTitle != null) {
      dialogBuilder.setTitle(dialogTitle)
    } else {
      dialogBuilder.setTitle(R.string.info)
    }
    dialogBuilder.setMessage(dialogMsg)

    dialogBuilder.setPositiveButton(positiveBtnTitle
    ) { _, _ ->
      sendResult(Activity.RESULT_OK)

      if (listener != null) {
        listener!!.onClick(Activity.RESULT_OK)
      }
    }

    dialogBuilder.setNegativeButton(negativeBtnTitle
    ) { _, _ ->
      sendResult(Activity.RESULT_CANCELED)

      if (listener != null) {
        listener!!.onClick(Activity.RESULT_CANCELED)
      }
    }

    return dialogBuilder.create()
  }

  private fun sendResult(result: Int) {
    if (targetFragment == null) {
      return
    }
    targetFragment!!.onActivityResult(targetRequestCode, result, null)
  }

  /**
   * This interface can be used by an activity to receive results from the dialog when a user choose some button.
   */
  interface OnTwoWayDialogListener {
    /**
     * @param result Can be [Activity.RESULT_OK] if the user clicks the positive button, or
     * [Activity.RESULT_CANCELED] if the user clicks the negative button.
     */
    fun onClick(result: Int)
  }

  companion object {
    private val KEY_DIALOG_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_DIALOG_TITLE", TwoWayDialogFragment::class.java)
    private val KEY_DIALOG_MESSAGE =
        GeneralUtil.generateUniqueExtraKey("KEY_DIALOG_MESSAGE", TwoWayDialogFragment::class.java)
    private val KEY_POSITIVE_BUTTON_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_POSITIVE_BUTTON_TITLE", TwoWayDialogFragment::class.java)
    private val KEY_NEGATIVE_BUTTON_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_NEGATIVE_BUTTON_TITLE", TwoWayDialogFragment::class.java)
    private val KEY_IS_CANCELABLE =
        GeneralUtil.generateUniqueExtraKey("KEY_IS_CANCELABLE", TwoWayDialogFragment::class.java)

    @JvmOverloads
    @JvmStatic
    fun newInstance(dialogTitle: String, dialogMsg: String, isCancelable: Boolean = false): TwoWayDialogFragment {
      return newInstance(dialogTitle, dialogMsg, null, null, isCancelable)
    }

    @JvmStatic
    fun newInstance(dialogTitle: String, dialogMsg: String, positiveButtonTitle: String?,
                    negativeButtonTitle: String?, isCancelable: Boolean): TwoWayDialogFragment {
      val args = Bundle()
      args.putString(KEY_DIALOG_TITLE, dialogTitle)
      args.putString(KEY_DIALOG_MESSAGE, dialogMsg)
      args.putString(KEY_POSITIVE_BUTTON_TITLE, positiveButtonTitle)
      args.putString(KEY_NEGATIVE_BUTTON_TITLE, negativeButtonTitle)
      args.putBoolean(KEY_IS_CANCELABLE, isCancelable)
      val infoDialogFragment = TwoWayDialogFragment()
      infoDialogFragment.arguments = args

      return infoDialogFragment
    }
  }
}
