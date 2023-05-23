/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseUpdateKeyDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class UpdatePrivateKeyDialogFragment : BaseUpdateKeyDialogFragment() {
  private val args by navArgs<UpdatePrivateKeyDialogFragmentArgs>()

  override fun prepareTitleText(): String = getString(R.string.new_private_key_details)

  override fun onPositiveButtonClicked() {
    navController?.navigateUp()
    setFragmentResult(
      args.requestKey,
      bundleOf(KEY_NEW_PRIVATE_KEY to args.newPgpKeyDetails)
    )
  }

  override fun getNewPgpKeyDetails(): PgpKeyDetails = args.newPgpKeyDetails

  override fun getExpectedEmailAddress(): String {
    return args.existingPgpKeyDetails.getPrimaryInternetAddress()?.address ?: ""
  }

  override fun getAdditionalWarningText(): String {
    return when {
      args.newPgpKeyDetails.fingerprint != args.existingPgpKeyDetails.fingerprint -> {
        getString(R.string.fingerprint_mismatch_you_are_trying_to_import_different_key)
      }

      args.existingPgpKeyDetails.lastModified == args.newPgpKeyDetails.lastModified -> {
        getString(R.string.you_are_trying_to_import_the_same_key)
      }

      args.existingPgpKeyDetails.isNewerThan(args.newPgpKeyDetails) -> {
        getString(R.string.warning_existing_key_has_more_recent_signature)
      }

      else -> ""
    }
  }

  override fun isNewKeyAcceptable(): Boolean {
    return isExpectedEmailFound()
        && args.newPgpKeyDetails.fingerprint == args.existingPgpKeyDetails.fingerprint
        && args.newPgpKeyDetails.isNewerThan(args.existingPgpKeyDetails)
  }

  companion object {
    val KEY_NEW_PRIVATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_NEW_PRIVATE_KEY", UpdatePrivateKeyDialogFragment::class.java
    )
  }
}
