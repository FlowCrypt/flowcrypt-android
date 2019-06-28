/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.ui.activity.ImportPgpContactActivity
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter
import com.flowcrypt.email.util.UIUtil

/**
 * This Activity show information about contacts where has_pgp == true.
 *
 *
 * Clicking the delete button will remove a contact from the db. This is useful if the contact
 * now has a new public key attested: next time the user writes them, it will pull a new public key.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 17:35
 * E-mail: DenBond7@gmail.com
 */

class ContactsSettingsActivity : BaseSettingsActivity(), LoaderManager.LoaderCallbacks<Cursor>,
    ContactsListCursorAdapter.OnDeleteContactListener, View.OnClickListener {
  private lateinit var progressBar: View
  private lateinit var listView: ListView
  private lateinit var emptyView: View
  private var adapter: ContactsListCursorAdapter? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_contacts_settings

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.progressBar = findViewById(R.id.progressBar)
    this.listView = findViewById(R.id.listViewContacts)
    this.emptyView = findViewById(R.id.emptyView)
    this.adapter = ContactsListCursorAdapter(this, null, false, this)
    listView.adapter = adapter

    if (findViewById<View>(R.id.floatActionButtonImportPublicKey) != null) {
      findViewById<View>(R.id.floatActionButtonImportPublicKey).setOnClickListener(this)
    }

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return when (id) {
      R.id.loader_id_load_contacts_with_has_pgp_true -> {

        val uri = ContactsDaoSource().baseContentUri
        val selection = ContactsDaoSource.COL_HAS_PGP + " = ?"
        val selectionArgs = arrayOf("1")

        CursorLoader(this, uri, null, selection, selectionArgs, null)
      }

      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
    when (loader.id) {
      R.id.loader_id_load_contacts_with_has_pgp_true -> {
        UIUtil.exchangeViewVisibility(this, false, progressBar, listView)

        if (data != null && data.count > 0) {
          adapter!!.swapCursor(data)
          UIUtil.exchangeViewVisibility(this, false, emptyView, listView)
        } else {
          UIUtil.exchangeViewVisibility(this, true, emptyView, listView)
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    when (loader.id) {
      R.id.loader_id_load_contacts_with_has_pgp_true -> adapter!!.swapCursor(null)
    }
  }

  override fun onClick(email: String) {
    ContactsDaoSource().deletePgpContact(this, email)
    Toast.makeText(this, getString(R.string.the_contact_was_deleted, email), Toast.LENGTH_SHORT).show()
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.floatActionButtonImportPublicKey -> startActivityForResult(ImportPgpContactActivity.newIntent(this), 0)
    }
  }
}
