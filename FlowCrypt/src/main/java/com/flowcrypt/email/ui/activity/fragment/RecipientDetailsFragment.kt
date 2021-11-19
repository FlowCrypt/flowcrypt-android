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
import kotlinx.coroutines.flow.collect

/**
 * @author Denis Bondarenko
 *         Date: 11/19/21
 *         Time: 1:03 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientDetailsFragment : BaseFragment(), ListProgressBehaviour {
  private val args by navArgs<RecipientDetailsFragmentArgs>()
  private var binding: FragmentRecipientDetailsBinding? = null
  private val recipientDetailsViewModel: RecipientDetailsViewModel by viewModels {
    object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return RecipientDetailsViewModel(args.recipientEntity, requireActivity().application) as T
      }
    }
  }
  override val contentResourceId: Int = R.layout.fragment_recipient_details

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
            .actionRecipientDetailsFragmentToPublicKeyDetailsFragment(args.recipientEntity)
        )
      }
    })

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentRecipientDetailsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.recipient_detail)
    initViews()
    setupRecipientDetailsViewModel()
  }

  private fun setupRecipientDetailsViewModel() {
    lifecycleScope.launchWhenStarted {
      recipientDetailsViewModel.recipientPubKeysFlow.collect {
        if (it.isNullOrEmpty()) {
          showEmptyView()
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
