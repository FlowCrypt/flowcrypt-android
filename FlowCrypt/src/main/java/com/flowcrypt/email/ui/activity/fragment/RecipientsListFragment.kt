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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.databinding.FragmentRecipientsListBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.RecipientsRecyclerViewAdapter

/**
 * This fragment shows a list of contacts which have a public key.
 *
 * @author Denys Bondarenko
 */
class RecipientsListFragment : BaseFragment<FragmentRecipientsListBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentRecipientsListBinding.inflate(inflater, container, false)

  private val recipientsRecyclerViewAdapter: RecipientsRecyclerViewAdapter =
    RecipientsRecyclerViewAdapter(
      isDeleteEnabled = true,
      onRecipientActionsListener = object :
        RecipientsRecyclerViewAdapter.OnRecipientActionsListener {
        override fun onDeleteRecipient(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker) {
          recipientsViewModel.deleteRecipient(recipientEntityWithPgpMarker.toRecipientEntity())
          toast(getString(R.string.the_contact_was_deleted, recipientEntityWithPgpMarker.email))
        }

        override fun onRecipientClick(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker) {
          navController?.navigate(
            RecipientsListFragmentDirections
              .actionRecipientsListFragmentToRecipientDetailsFragment(
                recipientEntityWithPgpMarker.toRecipientEntity()
              )
          )
        }
      })
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  private var onlyWithPgp: Boolean = true
  private var searchPattern: String = ""
  private var switchView: SwitchCompat? = null

  override val emptyView: View?
    get() = binding?.emptyView
  override val progressView: View?
    get() = binding?.pB
  override val contentView: View?
    get() = binding?.rVRecipients
  override val statusView: View?
    get() = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupRecipientsViewModel()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_recipients_list, menu)
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
            recipientsViewModel.filterContacts(
              onlyWithPgp = switchView?.isChecked ?: true,
              searchPattern = searchPattern
            )
            return true
          }

          override fun onQueryTextChange(newText: String?): Boolean {
            searchPattern = newText ?: ""
            recipientsViewModel.filterContacts(
              onlyWithPgp = switchView?.isChecked ?: true,
              searchPattern = searchPattern
            )
            return true
          }
        })
        searchView.clearFocus()

        val item = menu.findItem(R.id.menuSwitch)
        switchView = item.actionView?.findViewById(R.id.switchShowOnlyEncryptedMessages)
        switchView?.isChecked = onlyWithPgp
        switchView?.setOnCheckedChangeListener { _, isChecked ->
          onlyWithPgp = isChecked
          recipientsViewModel.filterContacts(
            onlyWithPgp = onlyWithPgp,
            searchPattern = searchPattern
          )
        }
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  private fun initViews() {
    val manager = LinearLayoutManager(context)
    val decoration = DividerItemDecoration(context, manager.orientation)
    binding?.rVRecipients?.addItemDecoration(decoration)
    binding?.rVRecipients?.layoutManager = manager
    binding?.rVRecipients?.adapter = recipientsRecyclerViewAdapter

    binding?.fABtImportPublicKey?.setOnClickListener {
      navController?.navigate(
        RecipientsListFragmentDirections
          .actionRecipientsListFragmentToImportRecipientsFromSourceFragment()
      )
    }
  }

  private fun setupRecipientsViewModel() {
    recipientsViewModel.allContactsLiveData.observe(viewLifecycleOwner) {
      recipientsViewModel.filterContacts(
        onlyWithPgp = switchView?.isChecked ?: true,
        searchPattern = searchPattern
      )
    }

    recipientsViewModel.contactsWithPgpMarkerSearchLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@RecipientsListFragment)
          showProgress()
        }

        Result.Status.SUCCESS -> {
          if (it.data.isNullOrEmpty()) {
            showEmptyView()
          } else {
            recipientsRecyclerViewAdapter.submitList(it.data)
            showContent()
          }
          countingIdlingResource?.decrementSafely(this@RecipientsListFragment)
        }

        else -> {}
      }
    }

    recipientsViewModel.filterContacts(
      onlyWithPgp = switchView?.isChecked ?: true,
      searchPattern = searchPattern
    )
  }
}
