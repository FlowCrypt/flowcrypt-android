/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.adapter.ContactsRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This activity can be used for select single or multiply contacts (not implemented yet) from the local database. The
 * activity returns [ContactEntity] as a result.
 *
 * @author Denis Bondarenko
 * Date: 14.11.2017
 * Time: 17:23
 * E-mail: DenBond7@gmail.com
 */
class SelectContactsActivity : BaseBackStackActivity(),
  ContactsRecyclerViewAdapter.OnContactActionsListener, SearchView.OnQueryTextListener {

  private var progressBar: View? = null
  private var recyclerViewContacts: RecyclerView? = null
  private var emptyView: View? = null
  private val contactsRecyclerViewAdapter: ContactsRecyclerViewAdapter =
    ContactsRecyclerViewAdapter(false)
  private var searchPattern: String? = null
  private val contactsViewModel: ContactsViewModel by viewModels()

  @VisibleForTesting
  private val countingIdlingResourceForFilter = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  override val contentViewResourceId: Int
    get() = R.layout.activity_select_contact

  override val rootView: View
    get() = findViewById(R.id.content)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    contactsRecyclerViewAdapter.onContactActionsListener = this
    //todo-denbond7 need to fix this in the future. Not urgent
    //val isMultiply = intent.getBooleanExtra(KEY_EXTRA_IS_MULTIPLY, false)

    val title = intent.getStringExtra(KEY_EXTRA_TITLE)

    progressBar = findViewById(R.id.progressBar)
    emptyView = findViewById(R.id.emptyView)
    recyclerViewContacts = findViewById(R.id.recyclerViewContacts)
    val manager = LinearLayoutManager(this)
    val decoration = DividerItemDecoration(this, manager.orientation)
    val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, theme)
    drawable?.let { decoration.setDrawable(drawable) }
    recyclerViewContacts?.addItemDecoration(decoration)
    recyclerViewContacts?.layoutManager = manager
    recyclerViewContacts?.adapter = contactsRecyclerViewAdapter

    if (!TextUtils.isEmpty(title)) {
      supportActionBar?.title = title
    }

    setupContactsViewModel()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.activity_select_contact, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val searchItem = menu.findItem(R.id.menuSearch)
    val searchView = searchItem.actionView as SearchView
    if (!TextUtils.isEmpty(searchPattern)) {
      searchItem.expandActionView()
    }
    searchView.setQuery(searchPattern, true)
    searchView.queryHint = getString(R.string.search)
    searchView.setOnQueryTextListener(this)
    searchView.clearFocus()
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onContactClick(contactEntity: ContactEntity) {
    val intent = Intent()
    intent.putExtra(KEY_EXTRA_PGP_CONTACT, contactEntity)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onDeleteContact(contactEntity: ContactEntity) {}

  override fun onQueryTextSubmit(query: String): Boolean {
    searchPattern = query
    contactsViewModel.filterContacts(searchPattern)
    return true
  }

  override fun onQueryTextChange(newText: String): Boolean {
    searchPattern = newText
    contactsViewModel.filterContacts(searchPattern)
    return true
  }

  private fun setupContactsViewModel() {
    contactsViewModel.allContactsLiveData.observe(this, {
      contactsViewModel.filterContacts(searchPattern)
    })

    contactsViewModel.contactsWithPgpSearchLiveData.observe(this, {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResourceForFilter.incrementSafely("searchPattern = $searchPattern")
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
          countingIdlingResourceForFilter.decrementSafely()
        }

        else -> {
          countingIdlingResourceForFilter.decrementSafely()
        }
      }
    })

    contactsViewModel.filterContacts(searchPattern)
  }

  companion object {
    val KEY_EXTRA_PGP_CONTACT =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_PGP_CONTACT",
        SelectContactsActivity::class.java
      )
    private val KEY_EXTRA_TITLE =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_TITLE", SelectContactsActivity::class.java)
    private val KEY_EXTRA_IS_MULTIPLY =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_IS_MULTIPLY",
        SelectContactsActivity::class.java
      )

    fun newIntent(context: Context?, title: String, isMultiply: Boolean): Intent {
      val intent = Intent(context, SelectContactsActivity::class.java)
      intent.putExtra(KEY_EXTRA_TITLE, title)
      intent.putExtra(KEY_EXTRA_IS_MULTIPLY, isMultiply)
      return intent
    }
  }
}
