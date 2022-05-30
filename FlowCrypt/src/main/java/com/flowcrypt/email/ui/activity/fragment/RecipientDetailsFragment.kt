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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentRecipientDetailsBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.RecipientDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.PubKeysRecyclerViewAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * @author Denis Bondarenko
 *         Date: 11/19/21
 *         Time: 1:03 PM
 *         E-mail: DenBond7@gmail.com
 */
@ExperimentalCoroutinesApi
class RecipientDetailsFragment : BaseFragment<FragmentRecipientDetailsBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentRecipientDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<RecipientDetailsFragmentArgs>()
  private val recipientDetailsViewModel: RecipientDetailsViewModel by viewModels {
    object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecipientDetailsViewModel(args.recipientEntity, requireActivity().application) as T
      }
    }
  }

  override val emptyView: View?
    get() = binding?.empty?.root
  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.rVPubKeys
  override val statusView: View?
    get() = binding?.status?.root

  private val pubKeysRecyclerViewAdapter = PubKeysRecyclerViewAdapter(
    object : PubKeysRecyclerViewAdapter.OnPubKeyActionsListener {
      override fun onPubKeyClick(publicKeyEntity: PublicKeyEntity) {
        navController?.navigate(
          RecipientDetailsFragmentDirections
            .actionRecipientDetailsFragmentToPublicKeyDetailsFragment(
              args.recipientEntity,
              publicKeyEntity
            )
        )
      }
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupRecipientDetailsViewModel()
  }

  private fun setupRecipientDetailsViewModel() {
    lifecycleScope.launchWhenStarted {
      recipientDetailsViewModel.recipientPubKeysFlow.collect {
        it ?: return@collect
        if (it.isNullOrEmpty()) {
          showEmptyView(resourcesId = R.drawable.ic_no_result_grey_24dp)
        } else {
          pubKeysRecyclerViewAdapter.submitList(it)
          showContent()
        }
      }
    }
  }

  private fun initViews() {
    binding?.tVName?.text = args.recipientEntity.name ?: "..."
    binding?.tVEmail?.text = args.recipientEntity.email

    binding?.rVPubKeys?.apply {
      setHasFixedSize(true)
      val manager = LinearLayoutManager(context)
      layoutManager = manager
      adapter = pubKeysRecyclerViewAdapter
    }
  }
}
