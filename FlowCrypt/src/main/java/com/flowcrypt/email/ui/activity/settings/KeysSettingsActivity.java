/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.PrivateKeyModel;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.PrivateKeyAdapter;
import com.flowcrypt.email.ui.loader.PreparePrivateKeyModelListAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

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

public class KeysSettingsActivity extends BaseBackStackActivity implements LoaderManager
        .LoaderCallbacks<LoaderResult>, View.OnClickListener {
    private static final int REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0;

    private View progressBar;
    private View emptyView;
    private View layoutContent;
    private ListView listViewKeys;

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
                        getSupportLoaderManager().restartLoader(R.id.loader_id_load_private_keys, null, this);
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_private_keys:
                return new PreparePrivateKeyModelListAsyncTaskLoader(this);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult data) {
        handleLoaderResult(loader, data);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_private_keys:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);

                List<PrivateKeyModel> privateKeyModelList = (List<PrivateKeyModel>) result;

                if (!privateKeyModelList.isEmpty()) {
                    listViewKeys.setAdapter(new PrivateKeyAdapter(this, privateKeyModelList));
                } else {
                    UIUtil.exchangeViewVisibility(this, true, emptyView, layoutContent);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_load_private_keys:
                finish();
                Toast.makeText(this, R.string.error_occurred_while_get_info_about_private_keys,
                        Toast.LENGTH_LONG).show();
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatActionButtonAddKey:
                startActivityForResult(ImportPrivateKeyActivity.newIntent(this, getString(R.string.import_private_key),
                        true, ImportPrivateKeyActivity.class), REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
                break;
        }
    }

    private void initViews() {
        this.progressBar = findViewById(R.id.progressBar);
        this.layoutContent = findViewById(R.id.layoutContent);
        this.emptyView = findViewById(R.id.emptyView);
        listViewKeys = findViewById(R.id.listViewKeys);

        if (findViewById(R.id.floatActionButtonAddKey) != null) {
            findViewById(R.id.floatActionButtonAddKey).setOnClickListener(this);
        }

        getSupportLoaderManager().initLoader(R.id.loader_id_load_private_keys, null, this);
    }
}
