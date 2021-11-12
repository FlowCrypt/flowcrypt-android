/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentParseAndSavePubKeysBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showInfoDialogWithExceptionDetails
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.CachedPubKeysKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.ImportOrUpdatePubKeysRecyclerViewAdapter
import kotlinx.coroutines.flow.collect

/**
 * @author Denis Bondarenko
 *         Date: 11/12/21
 *         Time: 4:09 PM
 *         E-mail: DenBond7@gmail.com
 */
class ParseAndSavePubKeysFragment : BaseFragment(), ListProgressBehaviour {
  private val args by navArgs<ParseAndSavePubKeysFragmentArgs>()
  private val parseKeysViewModel: ParseKeysViewModel by viewModels()
  private val cachedPubKeysKeysViewModel: CachedPubKeysKeysViewModel by viewModels()
  private var binding: FragmentParseAndSavePubKeysBinding? = null

  override val contentResourceId: Int = R.layout.fragment_parse_and_save_pub_keys

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
      override fun onSavePubKeyClick(pgpKeyDetails: PgpKeyDetails) {
        cachedPubKeysKeysViewModel.addPubKeysBasedOnPgpKeyDetails(pgpKeyDetails)
      }

      override fun onUpdatePubKeyClick(
        pgpKeyDetails: PgpKeyDetails,
        existingPublicKeyEntity: PublicKeyEntity
      ) {
        cachedPubKeysKeysViewModel.updateExistingPubKey(pgpKeyDetails, existingPublicKeyEntity)
      }
    })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    parseKeysViewModel.parseKeys(getSourceInputStreamFromArgs())
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentParseAndSavePubKeysBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.add_contact)
    initViews()

    setupParseKeysViewModel()
    setupCachedPubKeysKeysViewModel()
  }

  private fun initViews() {
    binding?.btImportAll?.setOnClickListener {
      cachedPubKeysKeysViewModel.importAllPubKeysWithConflictResolution(pubKeysAdapter.currentList)
    }

    binding?.rVPubKeys?.setHasFixedSize(true)
    binding?.rVPubKeys?.layoutManager = LinearLayoutManager(context)
    binding?.rVPubKeys?.adapter = pubKeysAdapter
  }

  private fun setupParseKeysViewModel() {
    lifecycleScope.launchWhenStarted {
      parseKeysViewModel.pgpKeyDetailsListStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
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
          }

          Result.Status.EXCEPTION -> {
            showStatus(
              getString(R.string.source_has_wrong_pgp_structure, getString(R.string.public_))
            )
            showInfoDialogWithExceptionDetails(it.exception)
          }

          else -> {
          }
        }
      }
    }
  }

  private fun setupCachedPubKeysKeysViewModel() {
    lifecycleScope.launchWhenStarted {
      cachedPubKeysKeysViewModel.filteredPubKeysStateFlow.collect {
        pubKeysAdapter.swap(it)
      }
    }

    lifecycleScope.launchWhenStarted {
      cachedPubKeysKeysViewModel.addPubKeysStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            toast(R.string.contact_successfully_saved)
          }

          Result.Status.EXCEPTION -> {
            showInfoDialogWithExceptionDetails(it.exception)
          }

          else -> {
          }
        }
      }
    }

    lifecycleScope.launchWhenStarted {
      cachedPubKeysKeysViewModel.updateExistingPubKeyStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            toast(R.string.contact_successfully_updated)
          }

          Result.Status.EXCEPTION -> {
            showInfoDialogWithExceptionDetails(it.exception)
          }

          else -> {
          }
        }
      }
    }

    lifecycleScope.launchWhenStarted {
      cachedPubKeysKeysViewModel.importAllPubKeysPubKeyStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            toast(R.string.success)
            navController?.navigateUp()
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
}
