/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.material.snackbar.Snackbar

/**
 * This activity describes a logic of import public keys.
 *
 * @author Denis Bondarenko
 * Date: 03.08.2017
 * Time: 12:35
 * E-mail: DenBond7@gmail.com
 */
class ImportPublicKeyActivity : BaseImportKeyActivity() {

  private var pgpContact: PgpContact? = null
  private val contactsViewModel: ContactsViewModel by viewModels()

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_public_key_for_pgp_contact

  override val isPrivateKeyMode: Boolean
    get() = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent != null && intent.hasExtra(KEY_EXTRA_PGP_CONTACT)) {
      this.pgpContact = intent.getParcelableExtra(KEY_EXTRA_PGP_CONTACT)
    } else {
      finish()
    }
  }

  override fun onKeyFound(
    sourceType: KeyImportDetails.SourceType,
    keyDetailsList: List<PgpKeyDetails>
  ) {
    if (keyDetailsList.isNotEmpty()) {
      if (keyDetailsList.size == 1) {
        val key = keyDetailsList.first()

        if (key.isPrivate) {
          showInfoSnackbar(
            rootView, getString(
              R.string.file_has_wrong_pgp_structure, getString(
                R
                  .string.public_
              )
            ), Snackbar.LENGTH_LONG
          )
          return
        }

        updateInformationAboutPgpContact(key)
        setResult(Activity.RESULT_OK)
        finish()
      } else {
        showInfoSnackbar(rootView, getString(R.string.select_only_one_key))
      }
    } else {
      showInfoSnackbar(rootView, getString(R.string.error_no_keys))
    }
  }

  private fun updateInformationAboutPgpContact(keyDetails: PgpKeyDetails) {
    val pgpContactFromKey = keyDetails.primaryPgpContact
    pgpContact?.pubkey = pgpContactFromKey.pubkey
    pgpContact?.let { contactsViewModel.updateContactPgpInfo(it, pgpContactFromKey) }
  }

  companion object {
    val KEY_EXTRA_PGP_CONTACT = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_PGP_CONTACT",
      ImportPublicKeyActivity::class.java
    )

    fun newIntent(
      context: Context?,
      accountEntity: AccountEntity?,
      title: String,
      pgpContact: PgpContact
    ): Intent {
      val intent = newIntent(
        context = context, accountEntity = accountEntity, title = title,
        throwErrorIfDuplicateFoundEnabled = false, cls = ImportPublicKeyActivity::class.java
      )
      intent.putExtra(KEY_EXTRA_PGP_CONTACT, pgpContact)
      return intent
    }
  }
}
