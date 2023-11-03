/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseUpdateKeyDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class UpdateRecipientPublicKeyDialogFragment : BaseUpdateKeyDialogFragment() {
  private val args by navArgs<UpdateRecipientPublicKeyDialogFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override fun prepareTitleText(): String = getString(R.string.public_key_details)

  override fun onPositiveButtonClicked() {
    recipientsViewModel.updateExistingPubKey(args.publicKeyEntity, args.pgpKeyRingDetails)
  }

  override fun getNewPgpKeyRingDetails(): PgpKeyRingDetails = args.pgpKeyRingDetails

  override fun getExpectedEmailAddress(): String = args.publicKeyEntity.recipient
  override fun getAdditionalWarningText(): String = ""

  override fun isNewKeyAcceptable(): Boolean = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    collectUpdateRecipientPublicKey()
  }

  private fun collectUpdateRecipientPublicKey() {
    launchAndRepeatWithLifecycle {
      recipientsViewModel.updateRecipientPublicKeyStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.let { isUpdated ->
              setFragmentResult(
                args.requestKey,
                bundleOf(KEY_UPDATE_RECIPIENT_PUBLIC_KEY to isUpdated)
              )
            }
            countingIdlingResource?.decrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            val exception = it.exception ?: return@collect
            val errorMsg = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            toast(errorMsg)
            countingIdlingResource?.decrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  companion object {
    val KEY_UPDATE_RECIPIENT_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_UPDATE_RECIPIENT_PUBLIC_KEY", UpdateRecipientPublicKeyDialogFragment::class.java
    )
  }
}
