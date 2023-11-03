/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentImportAllPubKeysFromSourceBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.CachedPubKeysKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.ImportPubKeysFromSourceSharedViewModel

/**
 * @author Denys Bondarenko
 */
class ImportAllPubKeysFromSourceDialogFragment : BaseDialogFragment() {
  private var binding: FragmentImportAllPubKeysFromSourceBinding? = null
  private val args by navArgs<ImportAllPubKeysFromSourceDialogFragmentArgs>()
  private val importPubKeysFromSourceSharedViewModel: ImportPubKeysFromSourceSharedViewModel
      by activityViewModels()
  private val cachedPubKeysKeysViewModel: CachedPubKeysKeysViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupImportPubKeysFromSourceSharedViewModel()
    setupCachedPubKeysKeysViewModel()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentImportAllPubKeysFromSourceBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun setupImportPubKeysFromSourceSharedViewModel() {
    launchAndRepeatWithLifecycle {
      importPubKeysFromSourceSharedViewModel.pgpKeyRingDetailsListStateFlow.collect {
        if (it.status == Result.Status.SUCCESS) {
          val pgpKeyDetailsList = it.data
          if (pgpKeyDetailsList.isNullOrEmpty()) {
            toast(R.string.unknown_error)
            navController?.navigateUp()
          } else {
            cachedPubKeysKeysViewModel.importAllPubKeysWithConflictResolution(pgpKeyDetailsList)
          }
        }
      }
    }
  }

  private fun setupCachedPubKeysKeysViewModel() {
    launchAndRepeatWithLifecycle {
      cachedPubKeysKeysViewModel.importAllPubKeysPubKeyStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            binding?.tVProgressTitle?.text = it.progressMsg

            if (it.progress != null) {
              if (binding?.pB?.isIndeterminate == true) {
                binding?.pB?.isIndeterminate = false
              }

              binding?.pB?.progress = it.progress.toInt()
            } else {
              countingIdlingResource?.incrementSafely(this@ImportAllPubKeysFromSourceDialogFragment)
            }
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            setFragmentResult(
              args.requestKey,
              bundleOf(KEY_IMPORT_PUB_KEYS_RESULT to it.data)
            )
            countingIdlingResource?.decrementSafely(this@ImportAllPubKeysFromSourceDialogFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  companion object {
    const val KEY_IMPORT_PUB_KEYS_RESULT = "KEY_IMPORT_PUB_KEYS_RESULT"
  }
}
