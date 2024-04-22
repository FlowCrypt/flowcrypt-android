/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentParseAndSavePubKeysBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialogWithExceptionDetails
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.jetpack.viewmodel.CachedPubKeysKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.ImportPubKeysFromSourceSharedViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ImportAllPubKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.adapter.ImportOrUpdatePubKeysRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class ParseAndSavePubKeysFragment : BaseFragment<FragmentParseAndSavePubKeysBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentParseAndSavePubKeysBinding.inflate(inflater, container, false)

  private val args by navArgs<ParseAndSavePubKeysFragmentArgs>()
  private val importPubKeysFromSourceSharedViewModel: ImportPubKeysFromSourceSharedViewModel
      by activityViewModels()
  private val cachedPubKeysKeysViewModel: CachedPubKeysKeysViewModel by viewModels()

  override val emptyView: View?
    get() = binding?.emptyView
  override val progressView: View?
    get() = binding?.groupProgress
  override val contentView: View?
    get() = binding?.groupContent
  override val statusView: View?
    get() = binding?.statusView?.root

  private val pubKeysAdapter = ImportOrUpdatePubKeysRecyclerViewAdapter(
    object : ImportOrUpdatePubKeysRecyclerViewAdapter.PubKeyActionsListener {
      override fun onSavePubKeyClick(pgpKeyRingDetails: PgpKeyRingDetails) {
        cachedPubKeysKeysViewModel.addPubKeysBasedOnPgpKeyDetails(pgpKeyRingDetails)
      }

      override fun onUpdatePubKeyClick(
        pgpKeyRingDetails: PgpKeyRingDetails,
        existingPublicKeyEntity: PublicKeyEntity
      ) {
        cachedPubKeysKeysViewModel.updateExistingPubKey(pgpKeyRingDetails, existingPublicKeyEntity)
      }
    })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subscribeToImportAllPubKeysFromSourceResult()
    importPubKeysFromSourceSharedViewModel.parseKeys(
      inputStream = getSourceInputStreamFromArgs(),
      skipErrors = true
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()

    setupImportPubKeysFromSourceSharedViewModel()
    setupCachedPubKeysKeysViewModel()
  }

  private fun initViews() {
    binding?.btImportAll?.setOnClickListener {
      navController?.navigate(
        ParseAndSavePubKeysFragmentDirections
          .actionParseAndSavePubKeysFragmentToImportAllPubKeysFromSourceDialogFragment(
            requestKey = REQUEST_KEY_IMPORT_PUB_KEYS
          )
      )
    }

    binding?.rVPubKeys?.setHasFixedSize(true)
    binding?.rVPubKeys?.layoutManager = LinearLayoutManager(context)
    binding?.rVPubKeys?.adapter = pubKeysAdapter
  }

  private fun setupImportPubKeysFromSourceSharedViewModel() {
    launchAndRepeatWithViewLifecycle {
      importPubKeysFromSourceSharedViewModel.pgpKeyRingDetailsListStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@ParseAndSavePubKeysFragment)
            showProgress()
          }

          Result.Status.SUCCESS -> {
            val pgpKeyDetailsList = it.data
            if (pgpKeyDetailsList.isNullOrEmpty()) {
              showEmptyView(getString(R.string.error_no_keys))
            } else {
              cachedPubKeysKeysViewModel.specifyFilter(pgpKeyDetailsList)
              pubKeysAdapter.submitList(pgpKeyDetailsList)
              showContent()
            }
            countingIdlingResource?.decrementSafely(this@ParseAndSavePubKeysFragment)
          }

          Result.Status.EXCEPTION -> {
            showStatus(
              getString(R.string.source_has_wrong_pgp_structure, getString(R.string.public_))
            )
            showInfoDialogWithExceptionDetails(it.exception)
            countingIdlingResource?.decrementSafely(this@ParseAndSavePubKeysFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  private fun setupCachedPubKeysKeysViewModel() {
    launchAndRepeatWithViewLifecycle {
      cachedPubKeysKeysViewModel.filteredPubKeysStateFlow.collect {
        pubKeysAdapter.swap(it)
      }
    }

    launchAndRepeatWithViewLifecycle {
      cachedPubKeysKeysViewModel.addPubKeysStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            toast(R.string.pub_key_successfully_imported)
          }

          Result.Status.EXCEPTION -> {
            showInfoDialogWithExceptionDetails(it.exception)
          }

          else -> {
          }
        }
      }
    }

    launchAndRepeatWithViewLifecycle {
      cachedPubKeysKeysViewModel.updateExistingPubKeyStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            toast(R.string.pub_key_successfully_updated)
          }

          Result.Status.EXCEPTION -> {
            showInfoDialogWithExceptionDetails(it.exception)
          }

          else -> {
          }
        }
      }
    }
  }

  private fun getSourceInputStreamFromArgs() = when {
    args.source?.isNotEmpty() == true -> {
      (args.source?.toByteArray() ?: byteArrayOf()).inputStream()
    }

    args.uri != null -> {
      try {
        val uri = requireNotNull(args.uri)
        requireContext().contentResolver.openInputStream(uri) ?: byteArrayOf().inputStream()
      } catch (e: Exception) {
        byteArrayOf().inputStream()
      }
    }

    else -> {
      byteArrayOf().inputStream()
    }
  }

  private fun subscribeToImportAllPubKeysFromSourceResult() {
    setFragmentResultListener(REQUEST_KEY_IMPORT_PUB_KEYS) { _, bundle ->
      val result =
        bundle.getBoolean(ImportAllPubKeysFromSourceDialogFragment.KEY_IMPORT_PUB_KEYS_RESULT)
      if (result) {
        toast(R.string.success)
        navController?.popBackStack(R.id.recipientsListFragment, false)
      }
    }
  }

  companion object {
    private val REQUEST_KEY_IMPORT_PUB_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_PUB_KEYS",
      ParseAndSavePubKeysFragment::class.java
    )
  }
}
