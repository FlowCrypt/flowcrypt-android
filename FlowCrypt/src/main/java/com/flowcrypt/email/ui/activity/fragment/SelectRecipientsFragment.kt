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
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
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
        override fun onDeleteRecipient(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker) {}

        override fun onRecipientClick(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker) {
          navController?.navigateUp()
          setFragmentResult(
            args.requestKey,
            bundleOf(KEY_RECIPIENTS to ArrayList(listOf(recipientEntityWithPgpMarker.toRecipientEntity())))
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
            recipientsViewModel.filterContacts(onlyWithPgp = true, searchPattern = searchPattern)
            return true
          }

          override fun onQueryTextChange(newText: String?): Boolean {
            searchPattern = newText ?: ""
            recipientsViewModel.filterContacts(onlyWithPgp = true, searchPattern = searchPattern)
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
      recipientsViewModel.filterContacts(onlyWithPgp = true, searchPattern = searchPattern)
    }

    recipientsViewModel.contactsWithPgpMarkerSearchLiveData.observe(viewLifecycleOwner) {
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

    recipientsViewModel.filterContacts(onlyWithPgp = true, searchPattern = searchPattern)
  }

  companion object {
    val KEY_RECIPIENTS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UPDATE_RECIPIENT_PUBLIC_KEY", SelectRecipientsFragment::class.java
    )
  }
}
