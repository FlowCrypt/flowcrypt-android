/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.model.DialogItem
import com.flowcrypt.email.ui.adapter.DialogItemAdapter
import com.flowcrypt.email.util.GeneralUtil
import java.util.ArrayList

/**
 * This dialog will be used to show for a user different options to resolve a PGP not found situation.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
class NoPgpFoundDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

  private var recipientWithPubKeys: RecipientWithPubKeys? = null
  private var dialogItems: MutableList<DialogItem> = mutableListOf()
  private var isRemoveActionEnabled: Boolean = false
  private var isProtectingWithPasswordEnabled: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.recipientWithPubKeys = arguments?.getParcelable(EXTRA_KEY_PGP_CONTACT)
    this.isRemoveActionEnabled = arguments?.getBoolean(EXTRA_KEY_IS_REMOVE_ACTION_ENABLED) ?: false
    this.isProtectingWithPasswordEnabled = arguments?.getBoolean(
      EXTRA_KEY_IS_PROTECTING_WITH_PASSWORD_ENABLED
    ) ?: false

    dialogItems = ArrayList()

    if (isProtectingWithPasswordEnabled) {
      dialogItems.add(
        DialogItem(
          iconResourceId = R.drawable.ic_password_protected_gray_48,
          title = getString(R.string.protect_with_password),
          id = RESULT_CODE_PROTECT_WITH_PASSWORD
        )
      )
    }

    dialogItems.add(
      DialogItem(
        iconResourceId = R.mipmap.ic_switch,
        title = getString(R.string.switch_to_standard_email),
        id = RESULT_CODE_SWITCH_TO_STANDARD_EMAIL
      )
    )
    dialogItems.add(
      DialogItem(
        iconResourceId = R.mipmap.ic_document,
        title = getString(R.string.import_their_public_key),
        id = RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY
      )
    )
    dialogItems.add(
      DialogItem(
        iconResourceId = R.mipmap.ic_content_copy,
        title = getString(R.string.copy_from_other_contact),
        id = RESULT_CODE_COPY_FROM_OTHER_CONTACT
      )
    )

    if (isRemoveActionEnabled) {
      dialogItems.add(
        DialogItem(
          iconResourceId = R.mipmap.ic_remove_recipient,
          title = getString(
            R.string.template_remove_recipient, recipientWithPubKeys?.recipient?.email
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
    val item = dialogItems[which]
    sendResult(item.id)
  }

  private fun sendResult(result: Int) {
    if (targetFragment == null) {
      return
    }

    val returnIntent = Intent()
    returnIntent.putExtra(EXTRA_KEY_PGP_CONTACT, recipientWithPubKeys)
    targetFragment?.onActivityResult(targetRequestCode, result, returnIntent)
  }

  companion object {
    const val RESULT_CODE_PROTECT_WITH_PASSWORD = 10
    const val RESULT_CODE_SWITCH_TO_STANDARD_EMAIL = 11
    const val RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY = 12
    const val RESULT_CODE_COPY_FROM_OTHER_CONTACT = 13
    const val RESULT_CODE_REMOVE_CONTACT = 14

    val EXTRA_KEY_PGP_CONTACT =
      GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_PGP_CONTACT",
        NoPgpFoundDialogFragment::class.java
      )

    private val EXTRA_KEY_IS_REMOVE_ACTION_ENABLED =
      GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_IS_REMOVE_ACTION_ENABLED",
        NoPgpFoundDialogFragment::class.java
      )

    private val EXTRA_KEY_IS_PROTECTING_WITH_PASSWORD_ENABLED =
      GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_IS_PROTECTING_WITH_PASSWORD_ENABLED",
        NoPgpFoundDialogFragment::class.java
      )

    fun newInstance(
      RecipientWithPubKeys: RecipientWithPubKeys,
      isRemoveActionEnabled: Boolean,
      isProtectingWithPasswordEnabled: Boolean = false
    ): NoPgpFoundDialogFragment {
      val args = Bundle()
      args.putParcelable(EXTRA_KEY_PGP_CONTACT, RecipientWithPubKeys)
      args.putBoolean(EXTRA_KEY_IS_REMOVE_ACTION_ENABLED, isRemoveActionEnabled)
      args.putBoolean(
        EXTRA_KEY_IS_PROTECTING_WITH_PASSWORD_ENABLED,
        isProtectingWithPasswordEnabled
      )
      val noPgpFoundDialogFragment = NoPgpFoundDialogFragment()
      noPgpFoundDialogFragment.arguments = args
      return noPgpFoundDialogFragment
    }
  }
}
