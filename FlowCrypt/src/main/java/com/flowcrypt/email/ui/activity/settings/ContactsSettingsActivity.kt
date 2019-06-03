/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.ui.activity.ImportPgpContactActivity;
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter;
import com.flowcrypt.email.util.UIUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/**
 * This Activity show information about contacts where has_pgp == true.
 * <p>
 * Clicking the delete button will remove a contact from the db. This is useful if the contact
 * now has a new public key attested: next time the user writes them, it will pull a new public key.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 17:35
 * E-mail: DenBond7@gmail.com
 */

public class ContactsSettingsActivity extends BaseSettingsActivity implements
    LoaderManager.LoaderCallbacks<Cursor>, ContactsListCursorAdapter.OnDeleteContactListener, View.OnClickListener {
  private View progressBar;
  private ListView listView;
  private View emptyView;
  private ContactsListCursorAdapter adapter;

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_contacts_settings;
  }

  @Override
  public View getRootView() {
    return null;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.progressBar = findViewById(R.id.progressBar);
    this.listView = findViewById(R.id.listViewContacts);
    this.emptyView = findViewById(R.id.emptyView);
    this.adapter = new ContactsListCursorAdapter(this, null, false, this);
    listView.setAdapter(adapter);

    if (findViewById(R.id.floatActionButtonImportPublicKey) != null) {
      findViewById(R.id.floatActionButtonImportPublicKey).setOnClickListener(this);
    }

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);
  }

  @Override
  @NonNull
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_contacts_with_has_pgp_true:

        Uri uri = new ContactsDaoSource().getBaseContentUri();
        String selection = ContactsDaoSource.COL_HAS_PGP + " = ?";
        String[] selectionArgs = new String[]{"1"};

        return new CursorLoader(this, uri, null, selection, selectionArgs, null);

      default:
        return new Loader<>(this);
    }
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
    switch (loader.getId()) {
      case R.id.loader_id_load_contacts_with_has_pgp_true:
        UIUtil.exchangeViewVisibility(this, false, progressBar, listView);

        if (data != null && data.getCount() > 0) {
          adapter.swapCursor(data);
          UIUtil.exchangeViewVisibility(this, false, emptyView, listView);
        } else {
          UIUtil.exchangeViewVisibility(this, true, emptyView, listView);
        }
        break;
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    switch (loader.getId()) {
      case R.id.loader_id_load_contacts_with_has_pgp_true:
        adapter.swapCursor(null);
        break;
    }
  }

  @Override
  public void onClick(String email) {
    new ContactsDaoSource().deletePgpContact(this, email);
    Toast.makeText(this, getString(R.string.the_contact_was_deleted, email), Toast.LENGTH_SHORT).show();
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.floatActionButtonImportPublicKey:
        startActivityForResult(ImportPgpContactActivity.newIntent(this), 0);
        break;
    }
  }
}
