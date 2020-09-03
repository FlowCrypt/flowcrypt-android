/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.extensions.showInfoDialogFragment
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.ui.activity.fragment.dialog.UpdatePublicKeyOfContactDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 8/31/20
 *         Time: 6:44 PM
 *         E-mail: DenBond7@gmail.com
 */
class EditContactActivity : BaseImportKeyActivity(), UpdatePublicKeyOfContactDialogFragment.OnKeySelectedListener {
  private val contactsViewModel: ContactsViewModel by viewModels()
  private var contactEntity: ContactEntity? = null
  private var editTextNewPubKey: EditText? = null

  override val contentViewResourceId: Int = R.layout.activity_edit_pgp_contact
  override val isPrivateKeyMode: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    contactEntity = intent?.getParcelableExtra(KEY_EXTRA_CONTACT)
    if (contactEntity == null) {
      finish()
      Toast.makeText(this, getString(R.string.contact_can_not_be_null), Toast.LENGTH_LONG).show()
    } else {
      supportActionBar?.title = contactEntity?.email
    }
  }

  override fun onPause() {
    super.onPause()
    isCheckingClipboardEnabled = false
  }

  override fun onKeyFound(type: KeyDetails.Type, keyDetailsList: ArrayList<NodeKeyDetails>) {
    if (keyDetailsList.size > 1) {
      showInfoDialogFragment(dialogMsg = getString(R.string.more_than_one_public_key_found))
      return
    }

    showDialogFragment(UpdatePublicKeyOfContactDialogFragment.newInstance(keyDetailsList.first()))
  }

  override fun initViews() {
    super.initViews()
    this.editTextNewPubKey = findViewById(R.id.editTextNewPubKey)

    findViewById<View>(R.id.buttonCheck)?.setOnClickListener {
      dismissSnackBar()

      if (true) {//check empty string
        keyImportModel = KeyImportModel(null, editTextNewPubKey?.text.toString(), isPrivateKeyMode, KeyDetails.Type.CLIPBOARD)
        keyImportModel?.let { privateKeysViewModel.parseKeys(it, false) }
      } else {

      }
    }
  }

  override fun onKeySelected(nodeKeyDetails: NodeKeyDetails) {
    contactsViewModel.updateContactPgpInfo(contactEntity, nodeKeyDetails)
    finish()
  }

  companion object {
    val KEY_EXTRA_CONTACT = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_CONTACT", EditContactActivity::class.java)

    fun newIntent(context: Context, accountEntity: AccountEntity?, contactEntity: ContactEntity?): Intent {
      return newIntent(
          context = context,
          accountEntity = accountEntity,
          title = context.getString(R.string.enter_a_new_public_key_for_this_contact),
          throwErrorIfDuplicateFoundEnabled = false,
          cls = EditContactActivity::class.java
      ).apply {
        putExtra(KEY_EXTRA_CONTACT, contactEntity)
      }
    }
  }
}
