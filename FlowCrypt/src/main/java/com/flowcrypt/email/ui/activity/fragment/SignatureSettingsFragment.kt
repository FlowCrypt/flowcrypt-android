/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentSignatureSettingsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.showKeyboard
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
    get() = binding?.content

  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupAccountAliasesViewModel()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      private var menuActionEdit: MenuItem? = null
      private var menuActionSave: MenuItem? = null

      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_signature_settings, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menuActionEdit = menu.findItem(R.id.menuActionEdit)
        menuActionSave = menu.findItem(R.id.menuActionSave)
        if (binding?.editTextSignature?.text?.isEmpty() == true) {
          menuActionEdit?.let { onMenuItemSelected(it) }
        }
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
          R.id.menuActionEdit -> {
            binding?.editTextSignature?.apply {
              isEnabled = true
              requestFocus()
              showKeyboard()
            }

            menuItem.isVisible = false
            menuActionSave?.isVisible = true
            true
          }

          R.id.menuActionSave -> {
            binding?.editTextSignature?.apply {
              isEnabled = false
              hideKeyboard()
            }

            menuItem.isVisible = false
            menuActionEdit?.isVisible = true
            true
          }

          else -> false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (binding?.editTextSignature?.text?.isEmpty() == true) {
      binding?.editTextSignature?.setText(accountEntity?.signature)
    }

    binding?.swipeRefreshLayout?.isEnabled = accountEntity?.isGoogleSignInAccount == true
    if (accountEntity?.isGoogleSignInAccount == true) {
      binding?.switchUseGmailAliases?.isChecked = account?.useAliasSignatures == true
      accountAliasesViewModel.fetchUpdates()
    } else {
      showContent()
    }
  }

  private fun initViews() {
    binding?.swipeRefreshLayout?.setColorSchemeResources(
      R.color.colorPrimary,
      R.color.colorPrimary,
      R.color.colorPrimary
    )
    binding?.swipeRefreshLayout?.setOnRefreshListener {
      accountAliasesViewModel.fetchUpdates(monitorProgress = true)
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

        binding?.editTextSignature?.apply {
          isEnabled = !isChecked
          if (isChecked) {
            hideKeyboard()
          }
        }

        binding?.recyclerViewAliasSignatures?.visibleOrGone(isChecked)
      }
    }
  }

  private fun setupAccountAliasesViewModel() {
    launchAndRepeatWithViewLifecycle {
      accountAliasesViewModel.fetchFreshestAliasesStateFlow.collect {
        binding?.swipeRefreshLayout?.isRefreshing = it.status == Result.Status.LOADING
      }
    }

    accountAliasesViewModel.accountAliasesLiveData.observe(viewLifecycleOwner) {
      signaturesListAdapter.submitList(it.filter { entity -> entity.signature?.isNotEmpty() == true })
      if (account?.useAliasSignatures == true) {
        binding?.editTextSignature?.apply {
          isEnabled = false
          hideKeyboard()
        }
      }
      binding?.groupAliasSignatures?.visibleOrGone(it.isNotEmpty())

      if (binding?.switchUseGmailAliases?.isChecked == false) {
        binding?.recyclerViewAliasSignatures?.gone()
      }

      showContent()
    }
  }
}
