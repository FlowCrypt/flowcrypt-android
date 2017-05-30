package com.flowcrypt.email.ui.activity.settings;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.ui.adapter.ContactsListCursorAdapter;
import com.flowcrypt.email.util.UIUtil;

/**
 * This Activity show information about contacts where has_pgp == true.
 * <p>
 * Clicking the delete button will remove a contact from the db. This is useful if the contact
 * now has a new public key attested: next time the user writes them, it will pull a new public key.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 17:35
 *         E-mail: DenBond7@gmail.com
 */

public class ContactsSettingsActivity extends BaseSettingsActivity implements LoaderManager
        .LoaderCallbacks<Cursor>, ContactsListCursorAdapter.OnDeleteContactButtonClickListener {
    private View progressBar;
    private ListView listViewContacts;
    private View emptyView;
    private ContactsListCursorAdapter contactsListCursorAdapter;

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_contacts_settings;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.progressBar = findViewById(R.id.progressBar);
        this.listViewContacts = (ListView) findViewById(R.id.listViewContacts);
        this.emptyView = findViewById(R.id.emptyView);
        this.contactsListCursorAdapter = new
                ContactsListCursorAdapter(this, null, false, this);
        listViewContacts.setAdapter(contactsListCursorAdapter);

        getSupportLoaderManager().initLoader(R.id.loader_id_load_contacts_with_has_pgp_true,
                null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:

                return new CursorLoader(this, new ContactsDaoSource().
                        getBaseContentUri(), null, ContactsDaoSource.COL_HAS_PGP +
                        " = ?", new String[]{"1"}, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                UIUtil.showProgress(this, false, progressBar, listViewContacts);

                if (data != null && data.getCount() > 0) {
                    contactsListCursorAdapter.swapCursor(data);
                } else {
                    UIUtil.showProgress(this, true, emptyView, listViewContacts);
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
    public void onDeleteContactButtonClick(String email) {
        new ContactsDaoSource().deletePgpContact(this, email);
        Toast.makeText(this, getString(R.string.the_contact_was_deleted, email), Toast
                .LENGTH_SHORT).show();
        getSupportLoaderManager().restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true,
                null, this);
    }
}
