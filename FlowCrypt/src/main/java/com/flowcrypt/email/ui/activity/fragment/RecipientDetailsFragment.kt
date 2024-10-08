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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentRecipientDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.RecipientDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.PubKeysRecyclerViewAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * @author Denys Bondarenko
 */
class RecipientDetailsFragment : BaseFragment<FragmentRecipientDetailsBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentRecipientDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<RecipientDetailsFragmentArgs>()
  private val recipientDetailsViewModel: RecipientDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
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

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun setupRecipientDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      recipientDetailsViewModel.recipientPubKeysFlow.collect {
        it ?: return@collect
        if (it.isEmpty()) {
          showEmptyView(imageResourcesId = R.drawable.ic_no_result_grey_24dp)
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
      val decoration = DividerItemDecoration(context, manager.orientation)
      addItemDecoration(decoration)
      layoutManager = manager
      adapter = pubKeysRecyclerViewAdapter
    }
  }
}
