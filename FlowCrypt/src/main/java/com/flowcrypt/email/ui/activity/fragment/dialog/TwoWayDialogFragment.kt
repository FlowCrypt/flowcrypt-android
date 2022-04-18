/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This dialog can be used if we need to show a simple info dialog
 * that has two buttons (negative and positive).
 *
 * @author Denis Bondarenko
 * Date: 28.08.2018
 * Time: 15:28
 * E-mail: DenBond7@gmail.com
 */
class TwoWayDialogFragment : BaseDialogFragment() {
  private val args by navArgs<TwoWayDialogFragmentArgs>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = args.isCancelable
  }

  override fun onStart() {
    super.onStart()
    modifyLinkMovementMethod(args.hasHtml, args.useLinkify)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialogBuilder = AlertDialog.Builder(requireContext())

    dialogBuilder.setTitle(args.dialogTitle)
    val msg = when {
      args.hasHtml -> UIUtil.getHtmlSpannedFromText(args.dialogMsg)
      else -> args.dialogMsg
    }
    dialogBuilder.setMessage(msg)

    dialogBuilder.setPositiveButton(
      args.positiveButtonTitle
    ) { _, _ ->
      navController?.navigateUp()
      setFragmentResult(
        REQUEST_KEY_BUTTON_CLICK,
        bundleOf(KEY_REQUEST_CODE to args.requestCode, KEY_RESULT to RESULT_OK)
      )
    }

    dialogBuilder.setNegativeButton(
      args.negativeButtonTitle
    ) { _, _ ->
      navController?.navigateUp()
      setFragmentResult(
        REQUEST_KEY_BUTTON_CLICK,
        bundleOf(KEY_REQUEST_CODE to args.requestCode, KEY_RESULT to RESULT_CANCELED)
      )
    }

    return dialogBuilder.create()
  }

  companion object {
    val REQUEST_KEY_BUTTON_CLICK = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_BUTTON_CLICK",
      TwoWayDialogFragment::class.java
    )

    val KEY_REQUEST_CODE = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_CODE", TwoWayDialogFragment::class.java
    )

    val KEY_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_RESULT", TwoWayDialogFragment::class.java
    )

    /** Standard activity result: operation canceled.  */
    const val RESULT_CANCELED = 0

    /** Standard activity result: operation succeeded. */
    const val RESULT_OK = 1
  }
}
