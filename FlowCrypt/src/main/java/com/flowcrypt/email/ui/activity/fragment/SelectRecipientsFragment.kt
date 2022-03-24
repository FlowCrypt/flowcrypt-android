/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.databinding.FragmentSelectRecipientsBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.RecipientsRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 3/24/22
 *         Time: 3:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class SelectRecipientsFragment : BaseFragment(), ListProgressBehaviour {
  private var binding: FragmentSelectRecipientsBinding? = null
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

  override val contentResourceId: Int = R.layout.fragment_select_recipients
  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.recyclerViewContacts
  override val statusView: View? = null
  override val emptyView: View?
    get() = binding?.layoutEmpty?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentSelectRecipientsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.title

    initViews()
    setupRecipientsViewModel()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_select_recipients, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
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

  private fun initViews() {
    val manager = LinearLayoutManager(requireContext())
    val decoration = DividerItemDecoration(requireContext(), manager.orientation)
    val drawable =
      ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, requireActivity().theme)
    drawable?.let { decoration.setDrawable(drawable) }
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
          countingIdlingResource.incrementSafely("searchPattern = $searchPattern")
          showProgress()
        }

        Result.Status.SUCCESS -> {
          if (it.data.isNullOrEmpty()) {
            showEmptyView()
          } else {
            recipientsRecyclerViewAdapter.submitList(it.data)
            showContent()
          }
          countingIdlingResource.decrementSafely()
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
