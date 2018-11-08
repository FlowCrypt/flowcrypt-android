/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.sun.mail.imap.protocol.SearchSequence;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

/**
 * This {@link android.app.Activity} searches and displays messages.
 *
 * @author Denis Bondarenko
 * Date: 26.04.2018
 * Time: 16:23
 * E-mail: DenBond7@gmail.com
 */
public class SearchMessagesActivity extends BaseEmailListActivity implements SearchView.OnQueryTextListener,
    MenuItem.OnActionExpandListener {
  public static final String SEARCH_FOLDER_NAME = "";
  public static final String EXTRA_KEY_QUERY = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_QUERY", SearchMessagesActivity.class);
  public static final String EXTRA_KEY_FOLDER = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_FOLDER", SearchMessagesActivity.class);

  private SearchView searchView;
  private AccountDao accountDao;
  private String initQuery;
  private Folder folder;

  public static Intent newIntent(Context context, String query, Folder folder) {
    Intent intent = new Intent(context, SearchMessagesActivity.class);
    intent.putExtra(EXTRA_KEY_QUERY, query);
    intent.putExtra(EXTRA_KEY_FOLDER, folder);
    return intent;
  }

  @Override
  public boolean isSyncEnable() {
    return true;
  }

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return true;
  }

  @Override
  public void refreshFoldersInfoFromCache() {

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //// TODO-denbond7: 26.04.2018 Need to add saving the query and restoring it
    this.accountDao = new AccountDaoSource().getActiveAccountInformation(this);
    if (getIntent() != null && getIntent().hasExtra(EXTRA_KEY_FOLDER)) {
      this.initQuery = getIntent().getStringExtra(EXTRA_KEY_QUERY);
      this.folder = getIntent().getParcelableExtra(EXTRA_KEY_FOLDER);
      if (folder != null) {
        folder.setFolderAlias(SEARCH_FOLDER_NAME);
        folder.setSearchQuery(initQuery);
      }
    } else {
      finish();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
    switch (requestCode) {
      case R.id.sync_request_code_search_messages:
        super.onReplyFromServiceReceived(R.id.syns_request_code_load_next_messages, resultCode, obj);
        break;

      default:
        super.onReplyFromServiceReceived(requestCode, resultCode, obj);
        break;
    }
  }

  @Override
  public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
    switch (requestCode) {
      case R.id.sync_request_code_search_messages:
        if (!countingIdlingResourceForMessages.isIdleNow()) {
          countingIdlingResourceForMessages.decrement();
        }
        notifyEmailListFragmentAboutError(requestCode, errorType, e);
        break;
    }
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_search_messages;
  }

  @Override
  public View getRootView() {
    return null;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_search_messages, menu);

    MenuItem menuItemSearch = menu.findItem(R.id.menuSearch);
    menuItemSearch.expandActionView();

    menuItemSearch.setOnActionExpandListener(this);

    searchView = (SearchView) menuItemSearch.getActionView();
    searchView.setQuery(initQuery, true);
    searchView.setQueryHint(getString(R.string.search));
    searchView.setOnQueryTextListener(this);

    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    if (searchManager != null) {
      searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }
    searchView.clearFocus();

    return true;
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    this.initQuery = query;

    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())
        && !SearchSequence.isAscii(query)) {
      Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show();
      return true;
    }

    folder.setSearchQuery(initQuery);
    updateEmailsListFragmentAfterFolderChange();
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    this.initQuery = newText;
    return false;
  }

  @Override
  public AccountDao getCurrentAccountDao() {
    return accountDao;
  }

  @Override
  public Folder getCurrentFolder() {
    return folder;
  }

  @Override
  public void onRetryGoogleAuth() {

  }

  @Override
  public boolean onMenuItemActionExpand(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuSearch:
        return true;

      default:
        return false;
    }
  }

  @Override
  public boolean onMenuItemActionCollapse(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuSearch:
        finish();
        return false;

      default:
        return false;
    }
  }
}
