/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentPrivateKeysBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.PrivateKeysListAdapter
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.pgpainless.key.info.KeyRingInfo

/**
 * This [Fragment] shows information about available private keys in the database.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysListFragment : BaseFragment<FragmentPrivateKeysBinding>(), ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPrivateKeysBinding.inflate(inflater, container, false)

  private val privateKeysAdapter = PrivateKeysListAdapter(
    onKeySelectedListener = object : PrivateKeysListAdapter.OnKeySelectedListener {
      override fun onKeySelected(position: Int, pgpKeyRingDetails: KeyRingInfo?) {
        pgpKeyRingDetails?.let {
          navController?.navigate(
            PrivateKeysListFragmentDirections
              .actionPrivateKeysListFragmentToPrivateKeyDetailsFragment(it.fingerprint.toString())
          )
        }
      }
    }
  )

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val emptyView: View?
    get() = binding?.emptyView
  override val progressView: View?
    get() = binding?.progressBar
  override val contentView: View?
    get() = binding?.content
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupPrivateKeysViewModel()
    subscribeToImportingAdditionalPrivateKeys()
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (accountEntity?.clientConfiguration?.usesKeyManager() == true) {
      binding?.floatActionButtonAddKey?.gone()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun setupPrivateKeysViewModel() {
    launchAndRepeatWithViewLifecycle {
      privateKeysViewModel.secretKeyRingsInfoStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
            countingIdlingResource?.incrementSafely(this@PrivateKeysListFragment)
          }

          Result.Status.SUCCESS -> {
            val detailsList = it.data ?: emptyList()
            privateKeysAdapter.submitList(detailsList)
            if (detailsList.isEmpty()) {
              showEmptyView()
            } else {
              showContent()
            }
            countingIdlingResource?.decrementSafely(this@PrivateKeysListFragment)
          }

          Result.Status.EXCEPTION -> {
            showContent()
            toast(it.exception?.message, Toast.LENGTH_SHORT)
            countingIdlingResource?.decrementSafely(this@PrivateKeysListFragment)
          }

          else -> {}
        }
      }
    }
  }

  private fun initViews() {
    binding?.recyclerViewKeys?.apply {
      setHasFixedSize(true)
      val manager = LinearLayoutManager(context)
      val decoration = DividerItemDecoration(context, manager.orientation)
      addItemDecoration(decoration)
      layoutManager = manager
      adapter = privateKeysAdapter
    }

    if (privateKeysAdapter.itemCount > 0) {
      showContent()
    }

    binding?.floatActionButtonAddKey?.setOnClickListener {
      account?.let { accountEntity ->
        navController?.navigate(
          object : NavDirections {
            override val actionId = R.id.import_additional_private_keys_graph
            override val arguments = ImportAdditionalPrivateKeysFragmentArgs(
              requestKey = REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS,
              accountEntity = accountEntity
            ).toBundle()
          }
        )
      }
    }
  }

  private fun subscribeToImportingAdditionalPrivateKeys() {
    setFragmentResultListener(REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt<PgpKeyRingDetails>(
        ImportAdditionalPrivateKeysFragment.KEY_IMPORTED_PRIVATE_KEYS
      )
      if (keys?.isNotEmpty() == true) {
        toast(R.string.key_successfully_imported)
      }
    }
  }

  companion object {
    private val REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS",
      PrivateKeysListFragment::class.java
    )
  }
}
