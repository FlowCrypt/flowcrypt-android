/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentSignatureSettingsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.viewmodel.AccountAliasesViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.adapter.SignaturesListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class SignatureSettingsFragment : BaseFragment<FragmentSignatureSettingsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentSignatureSettingsBinding.inflate(inflater, container, false)

  private val accountAliasesViewModel: AccountAliasesViewModel by viewModels()

  private val signaturesListAdapter: SignaturesListAdapter = SignaturesListAdapter()

  override val progressView: View?
    get() = binding?.progressBar

  override val contentView: View?
    get() = binding?.contentView

  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupAccountAliasesViewModel()
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    binding?.editTextSignature?.setText(accountEntity?.signature)
    binding?.swipeRefreshLayout?.isEnabled = accountEntity?.isGoogleSignInAccount == true
    if (accountEntity?.isGoogleSignInAccount == true) {
      binding?.switchUseGmailAliases?.isChecked = account?.useAliasSignatures == true
      accountAliasesViewModel.fetchUpdates()
    } else {
      showContent()
    }
  }

  private fun initViews() {
    binding?.swipeRefreshLayout?.apply {
      setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary)
      setOnRefreshListener { accountAliasesViewModel.fetchUpdates(monitorProgress = true) }
    }

    binding?.recyclerViewAliasSignatures?.apply {
      val manager = LinearLayoutManager(requireContext())
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_big)
        )
      )
      layoutManager = manager
      adapter = signaturesListAdapter
    }

    binding?.switchUseGmailAliases?.setOnCheckedChangeListener { _, isChecked ->
      lifecycleScope.launch {
        context?.applicationContext?.let { context ->
          val dao = FlowCryptRoomDatabase.getDatabase(context).accountDao()
          account?.id?.let { id -> dao.updateAccountAliasSignatureUsage(id, isChecked) }
        }

        binding?.recyclerViewAliasSignatures?.visibleOrGone(isChecked)
      }
    }

    binding?.signatureContainerForClick?.setOnClickListener {

    }
  }

  private fun setupAccountAliasesViewModel() {
    launchAndRepeatWithViewLifecycle {
      accountAliasesViewModel.fetchFreshestAliasesStateFlow.collect {
        binding?.swipeRefreshLayout?.isRefreshing = it.status == Result.Status.LOADING
      }
    }

    accountAliasesViewModel.accountAliasesLiveData.observe(viewLifecycleOwner) { entities ->
      val availableSignatures = entities.filter { entity ->
        entity.signature?.isNotEmpty() == true
      }
      signaturesListAdapter.submitList(availableSignatures)
      binding?.groupAliasSignatures?.visibleOrGone(availableSignatures.isNotEmpty())

      if (binding?.switchUseGmailAliases?.isChecked == false) {
        binding?.recyclerViewAliasSignatures?.gone()
      }

      showContent()
    }
  }
}
