/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.ui.adapter.DialogItemAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class ActionsDialogFragment : BaseDialogFragment() {
  private val args by navArgs<ActionsDialogFragmentArgs>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = args.isCancelable
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity).apply {
      setTitle(args.dialogTitle)
      setAdapter(
        DialogItemAdapter(
          requireContext(),
          args.items.toList()
        )
      ) { _: DialogInterface, which: Int ->
        navController?.navigateUp()
        setFragmentResult(
          args.requestKey,
          bundleOf(
            KEY_REQUEST_RESULT to args.items[which],
            KEY_REQUEST_INCOMING_BUNDLE to args.bundle
          )
        )
      }
    }.create()
  }

  companion object {
    val KEY_REQUEST_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_RESULT", ActionsDialogFragment::class.java
    )

    val KEY_REQUEST_INCOMING_BUNDLE = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_INCOMING_BUNDLE", ActionsDialogFragment::class.java
    )
  }
}