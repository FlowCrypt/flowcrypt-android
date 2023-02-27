/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.databinding.FragmentSelectRecipientsBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.RecipientsRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class SelectRecipientsFragment : BaseFragment<FragmentSelectRecipientsBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentSelectRecipientsBinding.inflate(inflater, container, false)

  private val args by navArgs<SelectRecipientsFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  private val recipientsRecyclerViewAdapter: RecipientsRecyclerViewAdapter =
    RecipientsRecyclerViewAdapter(
      isDeleteEnabled = false,
      onRecipientActionsListener =
      object : RecipientsRecyclerViewAdapter.OnRecipientActionsListener {
        override fun onDeleteRecipient(recipientEntity: RecipientEntity) {}

        override fun onRecipientClick(recipientEntity: RecipientEntity) {
          navController?.navigateUp()
          setFragmentResult(
            REQUEST_KEY_SELECT_RECIPIENTS,
            bundleOf(KEY_RECIPIENTS to ArrayList(listOf(recipientEntity)))
          )
        }
      })

  private var searchPattern: String = ""

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.recyclerViewContacts
  override val statusView: View? = null
  override val emptyView: View?
    get() = binding?.layoutEmpty?.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.title

    initViews()
    setupRecipientsViewModel()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_select_recipients, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val searchItem = menu.findItem(R.id.menuSearch)
        val searchView = searchItem.actionView as SearchView
        if (searchPattern.isNotEmpty()) {
          searchItem.expandActionView()
        }
        searchView.setQuery(searchPattern, true)
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
          override fun onQueryTextSubmit(query: String?): Boolean {
            searchPattern = query ?: ""
            recipientsViewModel.filterContacts(searchPattern)
            return true
          }

          override fun onQueryTextChange(newText: String?): Boolean {
            searchPattern = newText ?: ""
            recipientsViewModel.filterContacts(searchPattern)
            return true
          }
        })
        searchView.clearFocus()
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  private fun initViews() {
    val manager = LinearLayoutManager(requireContext())
    val decoration = DividerItemDecoration(requireContext(), manager.orientation)
    binding?.recyclerViewContacts?.addItemDecoration(decoration)
    binding?.recyclerViewContacts?.layoutManager = manager
    binding?.recyclerViewContacts?.adapter = recipientsRecyclerViewAdapter
  }

  private fun setupRecipientsViewModel() {
    recipientsViewModel.allContactsLiveData.observe(viewLifecycleOwner) {
      recipientsViewModel.filterContacts(searchPattern)
    }

    recipientsViewModel.contactsWithPgpSearchLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@SelectRecipientsFragment)
          showProgress()
        }

        Result.Status.SUCCESS -> {
          if (it.data.isNullOrEmpty()) {
            showEmptyView()
          } else {
            recipientsRecyclerViewAdapter.submitList(it.data)
            showContent()
          }
          countingIdlingResource?.decrementSafely(this@SelectRecipientsFragment)
        }

        else -> {}
      }
    }

    recipientsViewModel.filterContacts(searchPattern)
  }

  companion object {
    val REQUEST_KEY_SELECT_RECIPIENTS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_SELECT_RECIPIENTS", SelectRecipientsFragment::class.java
    )

    val KEY_RECIPIENTS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UPDATE_RECIPIENT_PUBLIC_KEY", SelectRecipientsFragment::class.java
    )
  }
}
