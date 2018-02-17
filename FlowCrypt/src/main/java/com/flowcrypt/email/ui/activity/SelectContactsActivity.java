/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This activity can be used for select single or multiply contacts (not implemented yet) from the local database. The
 * activity returns {@link PgpContact} as a result.
 *
 * @author Denis Bondarenko
 *         Date: 14.11.2017
 *         Time: 17:23
 *         E-mail: DenBond7@gmail.com
 */

public class SelectContactsActivity extends BaseBackStackActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {
    public static final String KEY_EXTRA_PGP_CONTACT =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PGP_CONTACT", SelectContactsActivity.class);

    public static final String KEY_EXTRA_PGP_CONTACTS =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PGP_CONTACTS", SelectContactsActivity.class);

    private static final String KEY_EXTRA_TITLE =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_TITLE", SelectContactsActivity.class);
    private static final String KEY_EXTRA_IS_MULTIPLY =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_MULTIPLY", SelectContactsActivity.class);

    private View progressBar;
    private ListView listViewContacts;
    private View emptyView;
    private ContactsListCursorAdapter contactsListCursorAdapter;
    private String userSearchPattern;

    public static Intent newIntent(Context context, String title, boolean isMultiply) {
        Intent intent = new Intent(context, SelectContactsActivity.class);
        intent.putExtra(KEY_EXTRA_TITLE, title);
        intent.putExtra(KEY_EXTRA_IS_MULTIPLY, isMultiply);
        return intent;
    }

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

        boolean isMultiply = getIntent().getBooleanExtra(KEY_EXTRA_IS_MULTIPLY, false);
        String title = getIntent().getStringExtra(KEY_EXTRA_TITLE);

        this.contactsListCursorAdapter = new ContactsListCursorAdapter(this, null, false, null, false);

        this.progressBar = findViewById(R.id.progressBar);
        this.emptyView = findViewById(R.id.emptyView);
        this.listViewContacts = findViewById(R.id.listViewContacts);
        this.listViewContacts.setAdapter(contactsListCursorAdapter);
        this.listViewContacts.setChoiceMode(isMultiply ? ListView.CHOICE_MODE_MULTIPLE : ListView.CHOICE_MODE_SINGLE);
        if (isMultiply) {
            //this.listViewContacts.setMultiChoiceModeListener(this);
        } else {
            this.listViewContacts.setOnItemClickListener(this);
        }

        if (!TextUtils.isEmpty(title) && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        getSupportLoaderManager().initLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_select_contact, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (!TextUtils.isEmpty(userSearchPattern)) {
            searchItem.expandActionView();
        }
        searchView.setQuery(userSearchPattern, true);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(this);
        searchView.clearFocus();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                String selection = ContactsDaoSource.COL_HAS_PGP + " = ?";
                String[] selectionArgs = new String[]{"1"};

                if (!TextUtils.isEmpty(userSearchPattern)) {
                    selection = ContactsDaoSource.COL_HAS_PGP + " = ? AND ( " + ContactsDaoSource.COL_EMAIL + " " +
                            "LIKE ? OR " + ContactsDaoSource.COL_NAME + " " + " LIKE ? )";
                    selectionArgs = new String[]{"1", "%" + userSearchPattern + "%", "%" + userSearchPattern + "%"};
                }

                return new CursorLoader(this, new ContactsDaoSource().
                        getBaseContentUri(), null, selection, selectionArgs, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                UIUtil.exchangeViewVisibility(this, false, progressBar, listViewContacts);

                if (data != null && data.getCount() > 0) {
                    emptyView.setVisibility(View.GONE);
                    contactsListCursorAdapter.swapCursor(data);
                } else {
                    UIUtil.exchangeViewVisibility(this, true, emptyView, listViewContacts);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                contactsListCursorAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
        PgpContact pgpContact = new ContactsDaoSource().getCurrentPgpContact(cursor);

        Intent intentResult = new Intent();
        intentResult.putExtra(KEY_EXTRA_PGP_CONTACT, pgpContact);
        setResult(RESULT_OK, intentResult);
        finish();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.userSearchPattern = query;
        getSupportLoaderManager().restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.userSearchPattern = newText;
        getSupportLoaderManager().restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);
        return true;
    }
}
