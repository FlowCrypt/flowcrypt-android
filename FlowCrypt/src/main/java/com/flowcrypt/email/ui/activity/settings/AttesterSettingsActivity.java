/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter;
import com.flowcrypt.email.ui.loader.LoadAccountKeysInfoFromAttester;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

/**
 * Basically, this Activity gets all known addresses of the user, and then submits one call with all addresses to
 * /lookup/email/ Attester API, then compares the results.
 *
 * @author DenBond7
 *         Date: 13.11.2017
 *         Time: 15:07
 *         E-mail: DenBond7@gmail.com
 */

public class AttesterSettingsActivity extends BaseBackStackActivity
        implements LoaderManager.LoaderCallbacks<LoaderResult> {
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
        return R.layout.activity_attester_settings;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.screenContent);
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_keys_info_from_attester:
                UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
                return new LoadAccountKeysInfoFromAttester(this,
                        new AccountDaoSource().getActiveAccountInformation(this));
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        handleLoaderResult(loader, loaderResult);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
        switch (loader.getId()) {
            default:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_keys_info_from_attester:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                List<LookUpEmailResponse> lookUpEmailResponses = (List<LookUpEmailResponse>) result;
                if (lookUpEmailResponses != null && !lookUpEmailResponses.isEmpty()) {
                    listViewKeys.setAdapter(new AttesterKeyAdapter(this, lookUpEmailResponses));
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
            case R.id.loader_id_load_keys_info_from_attester:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                showInfoSnackbar(getRootView(), e.getMessage());
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    private void initViews() {
        this.progressBar = findViewById(R.id.progressBar);
        this.layoutContent = findViewById(R.id.layoutContent);
        this.emptyView = findViewById(R.id.emptyView);
        listViewKeys = findViewById(R.id.listViewKeys);

        getSupportLoaderManager().initLoader(R.id.loader_id_load_keys_info_from_attester, null, this);
    }
}
