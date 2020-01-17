/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.widget.SearchView
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This activity can be used for select single or multiply contacts (not implemented yet) from the local database. The
 * activity returns [PgpContact] as a result.
 *
 * @author Denis Bondarenko
 * Date: 14.11.2017
 * Time: 17:23
 * E-mail: DenBond7@gmail.com
 */

class SelectContactsActivity : BaseBackStackActivity(), LoaderManager.LoaderCallbacks<Cursor>,
    AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {

  private var progressBar: View? = null
  private var listView: ListView? = null
  private var emptyView: View? = null
  private var adapter: ContactsListCursorAdapter? = null
  private var searchPattern: String? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_select_contact

  override val rootView: View
    get() = findViewById(R.id.content)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val isMultiply = intent.getBooleanExtra(KEY_EXTRA_IS_MULTIPLY, false)
    val title = intent.getStringExtra(KEY_EXTRA_TITLE)

    this.adapter = ContactsListCursorAdapter(this, null, false, null, false)

    this.progressBar = findViewById(R.id.progressBar)
    this.emptyView = findViewById(R.id.emptyView)
    this.listView = findViewById(R.id.listViewContacts)
    this.listView?.adapter = adapter
    this.listView?.choiceMode = if (isMultiply) ListView.CHOICE_MODE_MULTIPLE else ListView
        .CHOICE_MODE_SINGLE
    if (!isMultiply) {
      this.listView?.onItemClickListener = this
    }

    if (!TextUtils.isEmpty(title)) {
      supportActionBar?.title = title
    }

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_contacts_with_pgp, null, this)
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

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    when (id) {
      R.id.loader_id_load_contacts_with_pgp -> {
        var selection = ContactsDaoSource.COL_HAS_PGP + " = ?"
        var selectionArgs = arrayOf("1")

        if (!TextUtils.isEmpty(searchPattern)) {
          selection = ContactsDaoSource.COL_HAS_PGP + " = ? AND ( " + ContactsDaoSource.COL_EMAIL + " " +
              "LIKE ? OR " + ContactsDaoSource.COL_NAME + " " + " LIKE ? )"
          selectionArgs = arrayOf("1", "%$searchPattern%", "%$searchPattern%")
        }

        val uri = ContactsDaoSource().baseContentUri

        return CursorLoader(this, uri, null, selection, selectionArgs, null)
      }

      else -> return Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
    when (loader.id) {
      R.id.loader_id_load_contacts_with_pgp -> {
        UIUtil.exchangeViewVisibility(false, progressBar, listView)

        if (data != null && data.count > 0) {
          emptyView?.visibility = View.GONE
          adapter?.swapCursor(data)
        } else {
          UIUtil.exchangeViewVisibility(true, emptyView, listView)
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    when (loader.id) {
      R.id.loader_id_load_contacts_with_pgp -> adapter?.swapCursor(null)
    }
  }

  override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
    val cursor = parent.adapter.getItem(position) as Cursor
    val pgpContact = ContactsDaoSource().getCurrentPgpContact(cursor)

    val intentResult = Intent()
    intentResult.putExtra(KEY_EXTRA_PGP_CONTACT, pgpContact)
    setResult(Activity.RESULT_OK, intentResult)
    finish()
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    this.searchPattern = query
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_contacts_with_pgp, null, this)
    return true
  }

  override fun onQueryTextChange(newText: String): Boolean {
    this.searchPattern = newText
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_contacts_with_pgp, null, this)
    return true
  }

  companion object {
    val KEY_EXTRA_PGP_CONTACT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PGP_CONTACT", SelectContactsActivity::class.java)
    private val KEY_EXTRA_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_TITLE", SelectContactsActivity::class.java)
    private val KEY_EXTRA_IS_MULTIPLY =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_MULTIPLY", SelectContactsActivity::class.java)

    fun newIntent(context: Context?, title: String, isMultiply: Boolean): Intent {
      val intent = Intent(context, SelectContactsActivity::class.java)
      intent.putExtra(KEY_EXTRA_TITLE, title)
      intent.putExtra(KEY_EXTRA_IS_MULTIPLY, isMultiply)
      return intent
    }
  }
}
