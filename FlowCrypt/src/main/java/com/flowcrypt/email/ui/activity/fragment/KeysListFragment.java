/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.ui.adapter.PrivateKeysListCursorAdapter;
import com.flowcrypt.email.util.UIUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/**
 * This {@link Fragment} shows information about available private keys in the database.
 *
 * @author DenBond7
 * Date: 20.11.2018
 * Time: 10:30
 * E-mail: DenBond7@gmail.com
 */
public class KeysListFragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

  private static final int REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0;

  private View progressBar;
  private View emptyView;
  private View layoutContent;
  private PrivateKeysListCursorAdapter adapter;

  private LoaderManager.LoaderCallbacks<Cursor> callbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      switch (id) {
        case R.id.loader_id_load_contacts_with_has_pgp_true:
          return new CursorLoader(getContext(), new KeysDaoSource().getBaseContentUri(), null, null, null, null);

        default:
          return null;
      }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      switch (loader.getId()) {
        case R.id.loader_id_load_contacts_with_has_pgp_true:
          UIUtil.exchangeViewVisibility(getContext(), false, progressBar, layoutContent);

          if (data != null && data.getCount() > 0) {
            adapter.swapCursor(data);
          } else {
            UIUtil.exchangeViewVisibility(getContext(), true, emptyView, layoutContent);
          }
          break;
      }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      switch (loader.getId()) {
        case R.id.loader_id_load_contacts_with_has_pgp_true:
          adapter.swapCursor(null);
          break;
      }
    }
  };

  public static KeysListFragment newInstance() {
    return new KeysListFragment();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_private_keys, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(R.string.keys);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_START_IMPORT_KEY_ACTIVITY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Toast.makeText(getContext(), R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();
            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null,
                callbacks);
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
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

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Cursor cursor = (Cursor) parent.getItemAtPosition(position);
    String keyLongId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));

    if (!TextUtils.isEmpty(keyLongId)) {
      getFragmentManager()
          .beginTransaction()
          .replace(R.id.layoutContent, KeyDetailsFragment.newInstance(keyLongId))
          .addToBackStack(null)
          .commit();
    }
  }

  private void runCreateOrImportKeyActivity() {
    startActivityForResult(ImportPrivateKeyActivity.newIntent(getContext(), getString(R.string.import_private_key),
        true, ImportPrivateKeyActivity.class), REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
  }

  private void initViews(View root) {
    this.progressBar = root.findViewById(R.id.progressBar);
    this.layoutContent = root.findViewById(R.id.groupContent);
    this.emptyView = root.findViewById(R.id.emptyView);
    this.adapter = new PrivateKeysListCursorAdapter(getContext(), null);

    ListView listViewKeys = root.findViewById(R.id.listViewKeys);
    listViewKeys.setAdapter(adapter);
    listViewKeys.setOnItemClickListener(this);

    if (root.findViewById(R.id.floatActionButtonAddKey) != null) {
      root.findViewById(R.id.floatActionButtonAddKey).setOnClickListener(this);
    }

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_contacts_with_has_pgp_true, null, callbacks);
  }
}
