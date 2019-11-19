/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.ui.activity.ImportPgpContactActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter
import com.flowcrypt.email.util.UIUtil

/**
 * This fragment shows a list of contacts which have a public key.
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 6:11 PM
 *         E-mail: DenBond7@gmail.com
 */
class ContactsListFragment : BaseFragment(), ContactsListCursorAdapter.OnDeleteContactListener,
    View.OnClickListener, AdapterView.OnItemClickListener {

  private var progressBar: View? = null
  private var listView: ListView? = null
  private var emptyView: View? = null
  private var adapter: ContactsListCursorAdapter? = null

  private val cursorLoaderCallback = object : LoaderManager.LoaderCallbacks<Cursor> {
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
      return when (id) {
        R.id.loader_id_load_contacts_with_pgp -> {

          val uri = ContactsDaoSource().baseContentUri
          val selection = ContactsDaoSource.COL_HAS_PGP + " = ?"
          val selectionArgs = arrayOf("1")

          CursorLoader(context!!, uri, null, selection, selectionArgs, null)
        }

        else -> Loader(context!!)
      }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
      when (loader.id) {
        R.id.loader_id_load_contacts_with_pgp -> {
          UIUtil.exchangeViewVisibility(context!!, false, progressBar, listView)

          if (data != null && data.count > 0) {
            adapter!!.swapCursor(data)
            UIUtil.exchangeViewVisibility(context!!, false, emptyView, listView)
          } else {
            UIUtil.exchangeViewVisibility(context!!, true, emptyView, listView)
          }
        }
      }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
      when (loader.id) {
        R.id.loader_id_load_contacts_with_pgp -> adapter!!.swapCursor(null)
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_contacts_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    supportActionBar?.setTitle(R.string.contacts)
    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_contacts_with_pgp,
        null, cursorLoaderCallback)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> Toast.makeText(context, R.string.key_successfully_imported, Toast.LENGTH_SHORT).show()
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onContactDeleteClick(email: String) {
    ContactsDaoSource().deletePgpContact(context!!, email)
    Toast.makeText(context!!,
        getString(R.string.the_contact_was_deleted, email), Toast.LENGTH_SHORT).show()
    LoaderManager.getInstance(this)
        .restartLoader(R.id.loader_id_load_contacts_with_pgp, null, cursorLoaderCallback)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.floatActionButtonImportPublicKey ->
        context?.let {
          val accountDao = AccountDaoSource().getActiveAccountInformation(it) ?: return
          startActivityForResult(ImportPgpContactActivity.newIntent(it, accountDao),
              REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY)
        }

    }
  }

  override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    val cursor = (parent?.getItemAtPosition(position) as? Cursor)
    val email = cursor?.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL))
    val publicKey = cursor?.getString(cursor.getColumnIndex(ContactsDaoSource.COL_PUBLIC_KEY))

    parentFragmentManager
        .beginTransaction()
        .replace(R.id.layoutContent, PublicKeyDetailsFragment.newInstance(email, publicKey))
        .addToBackStack(null)
        .commit()
  }

  private fun initViews(root: View) {
    this.progressBar = root.findViewById(R.id.progressBar)
    this.listView = root.findViewById(R.id.listViewContacts)
    this.emptyView = root.findViewById(R.id.emptyView)
    this.adapter = ContactsListCursorAdapter(context!!, null, false, this)
    this.listView?.adapter = adapter
    this.listView?.onItemClickListener = this

    root.findViewById<View>(R.id.floatActionButtonImportPublicKey)?.setOnClickListener(this)
  }

  companion object {

    private const val REQUEST_CODE_START_IMPORT_PUB_KEY_ACTIVITY = 0

    @JvmStatic
    fun newInstance(): ContactsListFragment {
      return ContactsListFragment()
    }
  }
}