/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentImportMissingPublicKeyBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.material.snackbar.Snackbar

class ImportMissingPublicKeyFragment :
  BaseImportKeyFragment<FragmentImportMissingPublicKeyBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentImportMissingPublicKeyBinding.inflate(inflater, container, false)

  private val args by navArgs<ImportMissingPublicKeyFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override val isPrivateKeyMode: Boolean = false
  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    collectAddPublicKeyToRecipient()
  }

  override fun handleSelectedFile(uri: Uri) {
    showParsePgpKeysFromSourceDialogFragment(
      uri = uri,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    showParsePgpKeysFromSourceDialogFragment(
      source = pgpKeysAsString,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyDetails>) {
    if (keys.isNotEmpty()) {
      if (keys.size == 1) {
        val pgpKeyDetails = keys.first()

        if (!pgpKeyDetails.usableForEncryption) {
          showInfoSnackbar(
            view = binding?.root,
            msgText = getString(R.string.cannot_be_used_for_encryption),
            duration = Snackbar.LENGTH_LONG
          )
          return
        }

        if (pgpKeyDetails.isPrivate) {
          showInfoSnackbar(
            view = binding?.root,
            msgText = getString(R.string.file_has_wrong_pgp_structure, getString(R.string.public_)),
            duration = Snackbar.LENGTH_LONG
          )
          return
        }
        recipientsViewModel.copyPubKeysToRecipient(
          args.recipientWithPubKeys.recipient,
          pgpKeyDetails
        )
      } else {
        showInfoSnackbar(binding?.root, getString(R.string.select_only_one_key))
      }
    } else {
      showInfoSnackbar(binding?.root, getString(R.string.error_no_keys))
    }
  }

  private fun initViews() {
    binding?.buttonLoadFromClipboard?.setOnClickListener {
      showFindKeysInClipboardDialogFragment(false)
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun collectAddPublicKeyToRecipient() {
    lifecycleScope.launchWhenStarted {
      recipientsViewModel.addPublicKeyToRecipientStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.let { recipientWithPubKeys ->
              setFragmentResult(
                REQUEST_KEY_RECIPIENT_WITH_PUB_KEY,
                bundleOf(KEY_RECIPIENT_WITH_PUB_KEY to recipientWithPubKeys)
              )
            }
            countingIdlingResource?.decrementSafely()
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            val exception = it.exception ?: return@collect
            val errorMsg = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            showInfoSnackbar(msgText = errorMsg)
            countingIdlingResource?.decrementSafely()
          }
          else -> {
          }
        }
      }
    }
  }

  companion object {
    val REQUEST_KEY_RECIPIENT_WITH_PUB_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_RECIPIENT_WITH_PUB_KEY",
      ImportMissingPublicKeyFragment::class.java
    )

    val KEY_RECIPIENT_WITH_PUB_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_RECIPIENT_WITH_PUB_KEY", ImportMissingPublicKeyFragment::class.java
    )
  }
}
