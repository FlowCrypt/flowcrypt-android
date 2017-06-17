/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity;
import com.flowcrypt.email.ui.adapter.PrivateKeysListCursorAdapter;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;

/**
 * This Activity show information about available keys in the database.
 * <p>
 * Here we can import new keys.
 *
 * @author DenBond7
 *         Date: 29.05.2017
 *         Time: 11:30
 *         E-mail: DenBond7@gmail.com
 */

public class KeysSettingsActivity extends BaseSettingsActivity implements LoaderManager
        .LoaderCallbacks<Cursor>, View.OnClickListener {
    private View progressBar;
    private View emptyView;
    private View layoutContent;
    private PrivateKeysListCursorAdapter privateKeysListCursorAdapter;

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_keys_settings;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                return new CursorLoader(this, new KeysDaoSource().
                        getBaseContentUri(), null, null, null, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);

                if (data != null && data.getCount() > 0) {
                    privateKeysListCursorAdapter.swapCursor(data);
                } else {
                    UIUtil.exchangeViewVisibility(this, true, emptyView, layoutContent);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case R.id.loader_id_load_contacts_with_has_pgp_true:
                privateKeysListCursorAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatActionButtonAddKey:
                Intent intent = new Intent(this, CreateOrImportKeyActivity.class);
                intent.putExtra(CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON,
                        false);
                startActivity(intent);
                break;
        }
    }

    private void initViews() {
        try {
            Js js = new Js(this, new SecurityStorageConnector(this));
            this.progressBar = findViewById(R.id.progressBar);
            this.layoutContent = findViewById(R.id.layoutContent);
            this.emptyView = findViewById(R.id.emptyView);
            this.privateKeysListCursorAdapter = new PrivateKeysListCursorAdapter(this, null, js);
            ListView listViewKeys = (ListView) findViewById(R.id.listViewKeys);
            listViewKeys.setAdapter(privateKeysListCursorAdapter);

            if (findViewById(R.id.floatActionButtonAddKey) != null) {
                findViewById(R.id.floatActionButtonAddKey).setOnClickListener(this);
            }

            getSupportLoaderManager().initLoader(R.id.loader_id_load_contacts_with_has_pgp_true,
                    null, this);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can not load Js util.");
        }
    }
}
