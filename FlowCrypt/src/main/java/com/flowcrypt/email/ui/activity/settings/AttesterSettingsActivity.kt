/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter;
import com.flowcrypt.email.ui.loader.LoadAccountKeysInfo;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * Basically, this Activity gets all known addresses of the user, and then submits one call with all addresses to
 * /lookup/email/ Attester API, then compares the results.
 *
 * @author DenBond7
 * Date: 13.11.2017
 * Time: 15:07
 * E-mail: DenBond7@gmail.com
 */

public class AttesterSettingsActivity extends BaseBackStackActivity implements
    LoaderManager.LoaderCallbacks<LoaderResult> {
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
  @NonNull
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_keys_info_from_attester:
        UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
        return new LoadAccountKeysInfo(this,
            new AccountDaoSource().getActiveAccountInformation(this));
      default:
        return new Loader<>(this);
    }
  }

  @Override
  public void onLoadFinished(@NonNull Loader<LoaderResult> loader, LoaderResult loaderResult) {
    handleLoaderResult(loader, loaderResult);
  }

  @Override
  public void onLoaderReset(@NonNull Loader<LoaderResult> loader) {
    switch (loader.getId()) {
      default:
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_load_keys_info_from_attester:
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        List<LookUpEmailResponse> responses = (List<LookUpEmailResponse>) result;
        if (responses != null && !responses.isEmpty()) {
          listViewKeys.setAdapter(new AttesterKeyAdapter(this, responses));
        } else {
          UIUtil.exchangeViewVisibility(this, true, emptyView, layoutContent);
        }
        break;

      default:
        super.onSuccess(loaderId, result);
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_load_keys_info_from_attester:
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        showInfoSnackbar(getRootView(), e.getMessage());
        break;

      default:
        super.onError(loaderId, e);
    }
  }

  private void initViews() {
    this.progressBar = findViewById(R.id.progressBar);
    this.layoutContent = findViewById(R.id.layoutContent);
    this.emptyView = findViewById(R.id.emptyView);
    listViewKeys = findViewById(R.id.listViewKeys);

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_keys_info_from_attester, null, this);
  }
}
