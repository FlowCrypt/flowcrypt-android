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
  override fun preparePositiveButtonText(): String {
    return getString(R.string.use_this_key)
  }

  override fun prepareTitleText(): String = "New Private Key details"

  override fun onPositiveButtonClicked() {
    navController?.navigateUp()
    setFragmentResult(
      args.requestKey,
      bundleOf(KEY_NEW_PRIVATE_KEY to args.newPgpKeyDetails)
    )
  }

  override fun getOriginalPgpKeyDetails(): PgpKeyDetails = args.existingPgpKeyDetails

  override fun getExpectedEmailAddress(): String {
    return args.existingPgpKeyDetails.getPrimaryInternetAddress()?.address ?: ""
  }

  companion object {
    val KEY_NEW_PRIVATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_NEW_PRIVATE_KEY", UpdatePrivateKeyDialogFragment::class.java
    )
  }
}
