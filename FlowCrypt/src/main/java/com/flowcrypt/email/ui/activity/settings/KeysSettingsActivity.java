/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.app.Activity;
import android.content.Intent;
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
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.PrivateKeysListCursorAdapter;
import com.flowcrypt.email.util.UIUtil;

/**
 * This Activity show information about available keys in the database.
 * <p>
 * Here we can import new keys.
 *
 * @author DenBond7
 * Date: 29.05.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */

public class KeysSettingsActivity extends BaseBackStackActivity implements LoaderManager.LoaderCallbacks<Cursor>,
    View.OnClickListener {
  private static final int REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0;

  private View progressBar;
  private View emptyView;
  private View layoutContent;
  private PrivateKeysListCursorAdapter privateKeysListCursorAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_keys_settings;
  }

  @Override
  public View getRootView() {
    return null;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_START_IMPORT_KEY_ACTIVITY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Toast.makeText(this, R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();
            getSupportLoaderManager().restartLoader(R.id
                .loader_id_load_contacts_with_has_pgp_true, null, this);
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_contacts_with_has_pgp_true:
        return new CursorLoader(this, new KeysDaoSource().getBaseContentUri(), null, null, null, null);

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
        runCreateOrImportKeyActivity();
        break;
    }
  }

  private void runCreateOrImportKeyActivity() {
    startActivityForResult(ImportPrivateKeyActivity.newIntent(this, getString(R.string.import_private_key),
        true, ImportPrivateKeyActivity.class), REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
  }

  private void initViews() {
    this.progressBar = findViewById(R.id.progressBar);
    this.layoutContent = findViewById(R.id.layoutContent);
    this.emptyView = findViewById(R.id.emptyView);
    this.privateKeysListCursorAdapter = new PrivateKeysListCursorAdapter(this, null);

    ListView listViewKeys = findViewById(R.id.listViewKeys);
    listViewKeys.setAdapter(privateKeysListCursorAdapter);

    if (findViewById(R.id.floatActionButtonAddKey) != null) {
      findViewById(R.id.floatActionButtonAddKey).setOnClickListener(this);
    }

    getSupportLoaderManager().initLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, this);

  }
}
