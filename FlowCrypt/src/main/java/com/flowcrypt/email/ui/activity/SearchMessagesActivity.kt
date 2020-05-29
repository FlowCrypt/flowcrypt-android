/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivity
import com.flowcrypt.email.util.GeneralUtil
import com.sun.mail.imap.protocol.SearchSequence

/**
 * This [android.app.Activity] searches and displays messages.
 *
 * @author Denis Bondarenko
 * Date: 26.04.2018
 * Time: 16:23
 * E-mail: DenBond7@gmail.com
 */
class SearchMessagesActivity : BaseEmailListActivity(), SearchView.OnQueryTextListener,
    MenuItem.OnActionExpandListener {

  private var initQuery: String? = null
  override var currentFolder: LocalFolder? = null

  override val isSyncEnabled: Boolean
    get() = true

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = true

  override val contentViewResourceId: Int
    get() = R.layout.activity_search_messages

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    //// TODO-denbond7: 26.04.2018 Need to add saving the query and restoring it
    if (intent != null && intent.hasExtra(EXTRA_KEY_FOLDER)) {
      this.initQuery = intent.getStringExtra(EXTRA_KEY_QUERY)
      val incomingFolder: LocalFolder? = intent.getParcelableExtra(EXTRA_KEY_FOLDER)
      if (incomingFolder != null) {
        this.currentFolder = incomingFolder.copy(folderAlias = SEARCH_FOLDER_NAME,
            searchQuery = initQuery, msgCount = 0)
      }

      onFolderChanged(true)
    } else {
      finish()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        onBackPressed()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.sync_request_code_search_messages ->
        super.onReplyReceived(R.id.syns_request_code_load_next_messages, resultCode, obj)

      else -> {
      }
    }

    super.onReplyReceived(requestCode, resultCode, obj)
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.sync_request_code_search_messages -> {
        onErrorOccurred(requestCode, errorType, e)
      }
    }

    super.onErrorHappened(requestCode, errorType, e)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.activity_search_messages, menu)

    val menuItemSearch = menu.findItem(R.id.menuSearch)
    menuItemSearch.expandActionView()

    menuItemSearch.setOnActionExpandListener(this)

    val searchView = menuItemSearch.actionView as SearchView
    searchView.setQuery(initQuery, true)
    searchView.queryHint = getString(R.string.search)
    searchView.setOnQueryTextListener(this)

    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    searchView.clearFocus()

    return super.onCreateOptionsMenu(menu)
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    this.initQuery = query

    if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(activeAccount?.accountType, ignoreCase = true)
        && !SearchSequence.isAscii(query)) {
      Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show()
      return true
    }

    currentFolder?.searchQuery = initQuery
    onFolderChanged(true)
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    this.initQuery = newText
    return false
  }

  override fun onRetryGoogleAuth() {

  }

  override fun onMenuItemActionExpand(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuSearch -> true

      else -> false
    }
  }

  override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuSearch -> {
        finish()
        false
      }

      else -> false
    }
  }

  companion object {
    const val SEARCH_FOLDER_NAME = ""
    val EXTRA_KEY_QUERY = GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_QUERY", SearchMessagesActivity::class.java)
    val EXTRA_KEY_FOLDER = GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_FOLDER", SearchMessagesActivity::class.java)

    fun newIntent(context: Context, query: String, localFolder: LocalFolder?): Intent {
      val intent = Intent(context, SearchMessagesActivity::class.java)
      intent.putExtra(EXTRA_KEY_QUERY, query)
      intent.putExtra(EXTRA_KEY_FOLDER, localFolder)
      return intent
    }
  }
}
