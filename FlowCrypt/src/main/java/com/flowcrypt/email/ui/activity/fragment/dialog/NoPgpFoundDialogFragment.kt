/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.model.DialogItem
import com.flowcrypt.email.ui.adapter.DialogItemAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * This dialog will be used to show for a user different options to resolve a PGP not found situation.
 *
 * @author Denys Bondarenko
 */
class NoPgpFoundDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
  private val args by navArgs<NoPgpFoundDialogFragmentArgs>()
  private var dialogItems: MutableList<DialogItem> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dialogItems = ArrayList()

    dialogItems.add(
      DialogItem(
        iconResourceId = R.drawable.ic_refresh_gray,
        title = getString(R.string.re_fetch_public_key),
        id = RESULT_CODE_RE_FETCH_PUBLIC_KEY
      )
    )

    dialogItems.add(
      DialogItem(
        iconResourceId = R.drawable.ic_password_protected_gray,
        title = getString(R.string.protect_with_password),
        id = RESULT_CODE_PROTECT_WITH_PASSWORD
      )
    )

    dialogItems.add(
      DialogItem(
        iconResourceId = R.drawable.ic_switch,
        title = getString(R.string.switch_to_standard_email),
        id = RESULT_CODE_SWITCH_TO_STANDARD_EMAIL
      )
    )
    dialogItems.add(
      DialogItem(
        iconResourceId = R.drawable.ic_import_user_public_key,
        title = getString(R.string.import_their_public_key),
        id = RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY
      )
    )
    dialogItems.add(
      DialogItem(
        iconResourceId = R.drawable.ic_content_copy,
        title = getString(R.string.copy_from_other_contact),
        id = RESULT_CODE_COPY_FROM_OTHER_CONTACT
      )
    )

    if (args.isRemoveActionEnabled) {
      dialogItems.add(
        DialogItem(
          iconResourceId = R.drawable.ic_remove_recipient,
          title = getString(
            R.string.template_remove_recipient,
            args.recipientWithPubKeys.recipient.email
          ),
          id = RESULT_CODE_REMOVE_CONTACT
        )
      )
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)
    val dialogItemAdapter = DialogItemAdapter(requireContext(), dialogItems)

    builder.setTitle(R.string.recipient_does_not_use_pgp)
    builder.setAdapter(dialogItemAdapter, this)
    return builder.create()
  }

  override fun onClick(dialog: DialogInterface, which: Int) {
    navController?.navigateUp()
    setFragmentResult(
      args.requestKey,
      bundleOf(
        KEY_REQUEST_RESULT_CODE to dialogItems[which].id,
        KEY_REQUEST_RECIPIENT_WITH_PUB_KEYS to args.recipientWithPubKeys
      )
    )
  }

  companion object {
    val KEY_REQUEST_RESULT_CODE = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_RESULT_CODE", NoPgpFoundDialogFragment::class.java
    )

    val KEY_REQUEST_RECIPIENT_WITH_PUB_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_RECIPIENT_WITH_PUB_KEYS", NoPgpFoundDialogFragment::class.java
    )

    const val RESULT_CODE_PROTECT_WITH_PASSWORD = 10
    const val RESULT_CODE_SWITCH_TO_STANDARD_EMAIL = 11
    const val RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY = 12
    const val RESULT_CODE_COPY_FROM_OTHER_CONTACT = 13
    const val RESULT_CODE_REMOVE_CONTACT = 14
    const val RESULT_CODE_RE_FETCH_PUBLIC_KEY = 15
  }
}
