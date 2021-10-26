/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.ui.activity.ImportPgpContactActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.ContactsRecyclerViewAdapter
import com.flowcrypt.email.util.UIUtil

/**
 * This fragment shows a list of contacts which have a public key.
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 6:11 PM
 *         E-mail: DenBond7@gmail.com
 */
class ContactsListFragment : BaseFragment(), ContactsRecyclerViewAdapter.OnContactActionsListener {

  private var progressBar: View? = null
  private var recyclerViewContacts: RecyclerView? = null
  private var emptyView: View? = null
  private val contactsRecyclerViewAdapter: ContactsRecyclerViewAdapter =
    ContactsRecyclerViewAdapter(true)
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override val contentResourceId: Int = R.layout.fragment_contacts_list

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    contactsRecyclerViewAdapter.onContactActionsListener = this
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.contacts)
    initViews(view)
    setupContactsViewModel()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> Toast.makeText(
          context,
          R.string.key_successfully_imported,
          Toast.LENGTH_SHORT
        ).show()
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onContactClick(recipientEntity: RecipientEntity) {
    navController?.navigate(
      ContactsListFragmentDirections
        .actionContactsListFragmentToPublicKeyDetailsFragment(recipientEntity)
    )
  }

  override fun onDeleteContact(recipientEntity: RecipientEntity) {
    recipientsViewModel.deleteContact(recipientEntity)
    Toast.makeText(
      context, getString(R.string.the_contact_was_deleted, recipientEntity.email),
      Toast.LENGTH_SHORT
    ).show()
  }

  private fun initViews(root: View) {
    this.progressBar = root.findViewById(R.id.progressBar)
    this.emptyView = root.findViewById(R.id.emptyView)

    recyclerViewContacts = root.findViewById(R.id.recyclerViewContacts)
    val manager = LinearLayoutManager(context)
    val decoration = DividerItemDecoration(context, manager.orientation)
    val drawable =
      ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, requireContext().theme)
    drawable?.let { decoration.setDrawable(drawable) }
    recyclerViewContacts?.addItemDecoration(decoration)
    recyclerViewContacts?.layoutManager = manager
    recyclerViewContacts?.adapter = contactsRecyclerViewAdapter

    root.findViewById<View>(R.id.floatActionButtonImportPublicKey)?.setOnClickListener {
      context?.let {
        startActivityForResult(
          ImportPgpContactActivity.newIntent(it, account),
          REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY
        )
      }
    }
  }

  private fun setupContactsViewModel() {
    recipientsViewModel.contactsWithPgpLiveData.observe(viewLifecycleOwner, {
      when (it.status) {
        Result.Status.LOADING -> {
          UIUtil.exchangeViewVisibility(true, progressBar, recyclerViewContacts)
        }

        Result.Status.SUCCESS -> {
          UIUtil.exchangeViewVisibility(false, progressBar, recyclerViewContacts)
          if (it.data.isNullOrEmpty()) {
            UIUtil.exchangeViewVisibility(true, emptyView, recyclerViewContacts)
          } else {
            contactsRecyclerViewAdapter.swap(it.data)
            UIUtil.exchangeViewVisibility(false, emptyView, recyclerViewContacts)
          }
        }

        else -> {
        }
      }
    })
  }

  companion object {
    private const val REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY = 0
  }
}
