/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.BaseResponse;
import com.flowcrypt.email.api.retrofit.request.attester.LookUpRequest;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpResponse;
import com.flowcrypt.email.api.retrofit.response.model.LookUpPublicKeyInfo;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * This {@link Activity} retrieves a public keys string from the different sources and sends it to
 * {@link PreviewImportPgpContactActivity}
 *
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 17:07
 * E-mail: DenBond7@gmail.com
 */
public class ImportPgpContactActivity extends BaseImportKeyActivity implements TextView.OnEditorActionListener {
  private static final int REQUEST_CODE_RUN_PREVIEW_ACTIVITY = 100;
  private EditText editTextEmailOrId;

  private boolean isSearchingActiveNow;

  public static Intent newIntent(Context context) {
    return newIntent(context, context.getString(R.string.add_public_keys_of_your_contacts),
        false, ImportPgpContactActivity.class);
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_import_public_keys;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_import_public_keys, menu);
    return true;
  }

  @Override
  public void onPause() {
    super.onPause();
    isCheckingClipboardEnabled = false;
  }

  @Override
  public void onBackPressed() {
    if (isSearchingActiveNow) {
      this.isSearchingActiveNow = false;
      LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_search_public_key);
      UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_RUN_PREVIEW_ACTIVITY:
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionHelp:
        startActivity(new Intent(this, FeedbackActivity.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onKeyValidated(KeyDetails.Type type) {
    switch (type) {
      case CLIPBOARD:
        if (!keyDetailsList.isEmpty()) {
          UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
          startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, keyImportModel
              .getKeyString()), REQUEST_CODE_RUN_PREVIEW_ACTIVITY);
        } else {
          UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
          Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
        }
        break;
    }
  }

  @Override
  protected void handleSelectedFile(Uri uri) {
    if (uri != null) {
      UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
      startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, uri), REQUEST_CODE_RUN_PREVIEW_ACTIVITY);
    }
  }

  @Override
  public boolean isPrivateKeyMode() {
    return false;
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_search_public_key:
        this.isSearchingActiveNow = true;
        UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
        LookUpRequest lookUpRequest = new LookUpRequest(editTextEmailOrId.getText().toString());
        return new ApiServiceAsyncTaskLoader(getApplicationContext(), lookUpRequest);
      default:
        return super.onCreateLoader(id, args);
    }
  }

  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_search_public_key:
        this.isSearchingActiveNow = false;
        BaseResponse baseResponse = (BaseResponse) result;
        if (baseResponse != null) {
          if (baseResponse.getResponseModel() != null) {
            LookUpResponse lookUpResponse = (LookUpResponse) baseResponse.getResponseModel();
            if (lookUpResponse.getApiError() != null) {
              UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
              UIUtil.showInfoSnackbar(getRootView(), lookUpResponse.getApiError().getMessage());
            } else {
              ArrayList<LookUpPublicKeyInfo> lookUpPublicKeyInfoArrayList = lookUpResponse.getResults();
              if (lookUpPublicKeyInfoArrayList != null && !lookUpPublicKeyInfoArrayList.isEmpty()) {
                StringBuilder builder = new StringBuilder();

                for (LookUpPublicKeyInfo lookUpPublicKeyInfo : lookUpPublicKeyInfoArrayList) {
                  if (lookUpPublicKeyInfo != null) {
                    builder.append(lookUpPublicKeyInfo.getPubKey());
                  }
                }

                if (builder.length() > 0) {
                  startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, builder.toString()),
                      REQUEST_CODE_RUN_PREVIEW_ACTIVITY);
                } else {
                  UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
                  Toast.makeText(this, R.string.no_public_key_found, Toast.LENGTH_SHORT).show();
                }
              } else {
                UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
                UIUtil.showInfoSnackbar(getRootView(), getString(R.string.api_error));
              }
            }
          } else {
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
            UIUtil.showInfoSnackbar(getRootView(), getString(R.string.api_error));
          }
        } else {
          UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
          UIUtil.showInfoSnackbar(getRootView(), getString(R.string.internal_error));
        }
        break;

      default:
        super.onSuccess(loaderId, result);
    }
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    switch (actionId) {
      case EditorInfo.IME_ACTION_SEARCH:
        UIUtil.hideSoftInput(ImportPgpContactActivity.this, v);

        if (GeneralUtil.isInternetConnectionAvailable(this)) {
          LoaderManager.getInstance(this).restartLoader(R.id.loader_id_search_public_key, null,
              ImportPgpContactActivity.this);
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.internet_connection_is_not_available));
        }
        break;
    }

    return false;
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_search_public_key:
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
        Toast.makeText(this, TextUtils.isEmpty(e.getMessage()) ? getString(R.string.unknown_error) : e.getMessage(),
            Toast.LENGTH_SHORT).show();
        break;

      default:
        super.onError(loaderId, e);
    }
  }

  @Override
  protected void initViews() {
    super.initViews();
    this.editTextEmailOrId = findViewById(R.id.editTextKeyIdOrEmail);
    this.editTextEmailOrId.setOnEditorActionListener(this);
  }
}
