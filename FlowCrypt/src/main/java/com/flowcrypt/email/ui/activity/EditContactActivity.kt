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
import androidx.core.widget.addTextChangedListener
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.extensions.showInfoDialogFragment
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.ui.activity.fragment.dialog.UpdatePublicKeyOfContactDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 8/31/20
 *         Time: 6:44 PM
 *         E-mail: DenBond7@gmail.com
 */
class EditContactActivity : BaseImportKeyActivity(),
  UpdatePublicKeyOfContactDialogFragment.OnKeySelectedListener {
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private var publicKeyEntity: PublicKeyEntity? = null
  private var editTextNewPubKey: EditText? = null

  override val contentViewResourceId: Int = R.layout.activity_edit_pgp_contact
  override val isPrivateKeyMode: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    publicKeyEntity = intent?.getParcelableExtra(KEY_EXTRA_PUB_KEY)
    if (publicKeyEntity == null) {
      finish()
      Toast.makeText(this, getString(R.string.contact_can_not_be_null), Toast.LENGTH_LONG).show()
    } else {
      supportActionBar?.title = publicKeyEntity?.recipient
      supportActionBar?.subtitle = publicKeyEntity?.fingerprint
    }
  }

  override fun onPause() {
    super.onPause()
    isCheckingClipboardEnabled = false
  }

  override fun onKeyFound(
    sourceType: KeyImportDetails.SourceType,
    keyDetailsList: List<PgpKeyDetails>
  ) {
    if (keyDetailsList.size > 1) {
      showInfoDialogFragment(dialogMsg = getString(R.string.more_than_one_public_key_found))
      return
    }

    showDialogFragment(
      UpdatePublicKeyOfContactDialogFragment.newInstance(
        publicKeyEntity?.recipient,
        keyDetailsList.first()
      )
    )
  }

  override fun initViews() {
    super.initViews()
    editTextNewPubKey = findViewById(R.id.editTextNewPubKey)

    val buttonCheck = findViewById<View>(R.id.buttonCheck)
    editTextNewPubKey?.addTextChangedListener {
      buttonCheck?.isEnabled = !it.isNullOrEmpty()
    }

    buttonCheck?.setOnClickListener {
      dismissSnackBar()
      keyImportModel = KeyImportModel(
        null,
        editTextNewPubKey?.text.toString(),
        isPrivateKeyMode,
        KeyImportDetails.SourceType.MANUAL_ENTERING
      )
      keyImportModel?.let { privateKeysViewModel.parseKeys(it, false) }
    }
  }

  override fun onKeySelected(pgpKeyDetails: PgpKeyDetails) {
    publicKeyEntity?.let {
      recipientsViewModel.updateExistingPubKey(it, pgpKeyDetails)
    }
    finish()
  }

  companion object {
    val KEY_EXTRA_PUB_KEY =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUB_KEY", EditContactActivity::class.java)

    fun newIntent(
      context: Context,
      accountEntity: AccountEntity?,
      publicKeyEntity: PublicKeyEntity?
    ): Intent {
      return newIntent(
        context = context,
        accountEntity = accountEntity,
        title = context.getString(R.string.enter_a_new_public_key_for_this_contact),
        throwErrorIfDuplicateFoundEnabled = false,
        cls = EditContactActivity::class.java
      ).apply {
        putExtra(KEY_EXTRA_PUB_KEY, publicKeyEntity)
      }
    }
  }
}
