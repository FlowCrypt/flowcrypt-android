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
import com.flowcrypt.email.model.DialogItem
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.ui.adapter.DialogItemAdapter
import com.flowcrypt.email.util.GeneralUtil
import java.util.ArrayList

/**
 * This dialog will be used to show for user different options to resolve a PGP not found situation.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
class NoPgpFoundDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

  private var pgpContact: PgpContact? = null
  private var dialogItems: MutableList<DialogItem> = mutableListOf()
  private var isRemoveActionEnabled: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)


    this.pgpContact = arguments?.getParcelable(EXTRA_KEY_PGP_CONTACT)
    this.isRemoveActionEnabled = arguments?.getBoolean(EXTRA_KEY_IS_REMOVE_ACTION_ENABLED) ?: false

    dialogItems = ArrayList()

    dialogItems.add(
      DialogItem(
        R.mipmap.ic_switch, getString(R.string.switch_to_standard_email),
        RESULT_CODE_SWITCH_TO_STANDARD_EMAIL
      )
    )
    dialogItems.add(
      DialogItem(
        R.mipmap.ic_document, getString(R.string.import_their_public_key),
        RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY
      )
    )
    dialogItems.add(
      DialogItem(
        R.mipmap.ic_content_copy, getString(
          R.string
            .copy_from_other_contact
        ), RESULT_CODE_COPY_FROM_OTHER_CONTACT
      )
    )
    if (isRemoveActionEnabled) {
      dialogItems.add(
        DialogItem(
          R.mipmap.ic_remove_recipient, getString(
            R.string
              .template_remove_recipient, pgpContact!!.email
          ), RESULT_CODE_REMOVE_CONTACT
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
    returnIntent.putExtra(EXTRA_KEY_PGP_CONTACT, pgpContact)
    targetFragment?.onActivityResult(targetRequestCode, result, returnIntent)
  }

  companion object {
    const val RESULT_CODE_SWITCH_TO_STANDARD_EMAIL = 10
    const val RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY = 11
    const val RESULT_CODE_COPY_FROM_OTHER_CONTACT = 12
    const val RESULT_CODE_REMOVE_CONTACT = 13

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

    fun newInstance(
      pgpContact: PgpContact,
      isRemoveActionEnabled: Boolean
    ): NoPgpFoundDialogFragment {
      val args = Bundle()
      args.putParcelable(EXTRA_KEY_PGP_CONTACT, pgpContact)
      args.putBoolean(EXTRA_KEY_IS_REMOVE_ACTION_ENABLED, isRemoveActionEnabled)
      val noPgpFoundDialogFragment = NoPgpFoundDialogFragment()
      noPgpFoundDialogFragment.arguments = args
      return noPgpFoundDialogFragment
    }
  }
}
