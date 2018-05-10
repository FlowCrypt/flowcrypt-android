/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.BaseResponse;
import com.flowcrypt.email.api.retrofit.request.attester.LookUpRequest;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpResponse;
import com.flowcrypt.email.api.retrofit.response.model.LookUpPublicKeyInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter;
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.ParsePublicKeysFromStringAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 17:07
 * E-mail: DenBond7@gmail.com
 */
public class AddContactActivity extends BaseImportKeyActivity implements TextView.OnEditorActionListener {
    private View layoutPublicKeysContainer;
    private EditText editTextEmailOrId;
    private RecyclerView recyclerViewContacts;
    private TextView buttonImportAll;

    private String publicKeysString;
    private boolean isParsePublicKeysNow;
    private List<MessagePartPgpPublicKey> messagePartPgpPublicKeyList;

    public static Intent newIntent(Context context) {
        return newIntent(context, context.getString(R.string.add_public_keys_of_your_contacts),
                false, AddContactActivity.class);
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
    public void onBackPressed() {
        if (layoutPublicKeysContainer.getVisibility() == View.VISIBLE || isParsePublicKeysNow) {
            this.publicKeysString = null;
            this.isParsePublicKeysNow = false;
            getSupportLoaderManager().destroyLoader(R.id.loader_id_search_public_key);
            getSupportLoaderManager().destroyLoader(R.id.loader_id_parse_public_keys);
            layoutProgress.setVisibility(View.GONE);
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutPublicKeysContainer, layoutContentView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportAll:
                ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
                List<PgpContact> newCandidates = new ArrayList<>();
                List<PgpContact> updateCandidates = new ArrayList<>();

                for (MessagePartPgpPublicKey messagePartPgpPublicKey : messagePartPgpPublicKeyList) {
                    PgpContact pgpContact = new PgpContact(messagePartPgpPublicKey.getKeyOwner(),
                            null,
                            messagePartPgpPublicKey.getValue(),
                            true,
                            null,
                            false,
                            messagePartPgpPublicKey.getFingerprint(),
                            messagePartPgpPublicKey.getLongId(),
                            messagePartPgpPublicKey.getKeyWords(), 0);

                    if (messagePartPgpPublicKey.isPgpContactExists()) {
                        if (messagePartPgpPublicKey.isPgpContactCanBeUpdated()) {
                            updateCandidates.add(pgpContact);
                        }
                    } else {
                        newCandidates.add(pgpContact);
                    }
                }

                try {
                    contactsDaoSource.addRows(this, newCandidates);
                    contactsDaoSource.updatePgpContacts(this, updateCandidates);
                    Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } catch (RemoteException | OperationApplicationException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onClick(v);
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
        if (keyDetails != null) {
            this.publicKeysString = keyDetails.getValue();
            getSupportLoaderManager().restartLoader(R.id.loader_id_parse_public_keys, null, this);
        } else {
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean isPrivateKeyChecking() {
        return false;
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_search_public_key:
                this.isParsePublicKeysNow = true;
                UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
                return new ApiServiceAsyncTaskLoader(getApplicationContext(),
                        new LookUpRequest(editTextEmailOrId.getText().toString()));

            case R.id.loader_id_parse_public_keys:
                this.isParsePublicKeysNow = true;
                UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
                return new ParsePublicKeysFromStringAsyncTaskLoader(this, publicKeysString);

            default:
                return super.onCreateLoader(id, args);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_search_public_key:
                this.isParsePublicKeysNow = false;
                BaseResponse baseResponse = (BaseResponse) result;
                if (baseResponse != null) {
                    if (baseResponse.getResponseModel() != null) {
                        LookUpResponse lookUpResponse = (LookUpResponse) baseResponse.getResponseModel();
                        if (lookUpResponse.getApiError() != null) {
                            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress,
                                    layoutContentView);
                            UIUtil.showInfoSnackbar(getRootView(), lookUpResponse.getApiError().getMessage());
                        } else {
                            ArrayList<LookUpPublicKeyInfo> lookUpPublicKeyInfoArrayList = lookUpResponse.getResults();
                            if (lookUpPublicKeyInfoArrayList != null && !lookUpPublicKeyInfoArrayList.isEmpty()) {
                                StringBuilder stringBuilder = new StringBuilder();

                                for (LookUpPublicKeyInfo lookUpPublicKeyInfo : lookUpPublicKeyInfoArrayList) {
                                    if (lookUpPublicKeyInfo != null) {
                                        stringBuilder.append(lookUpPublicKeyInfo.getPublicKey());
                                    }
                                }

                                if (stringBuilder.length() > 0) {
                                    this.publicKeysString = stringBuilder.toString();
                                    getSupportLoaderManager().restartLoader(R.id.loader_id_parse_public_keys,
                                            null, this);
                                } else {
                                    displayPublicKeysViewIfCan(new ArrayList<MessagePartPgpPublicKey>());
                                }
                            } else {
                                UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress,
                                        layoutContentView);
                                UIUtil.showInfoSnackbar(getRootView(), getString(R.string.api_error));
                            }
                        }
                    } else {
                        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress,
                                layoutContentView);
                        UIUtil.showInfoSnackbar(getRootView(), getString(R.string.api_error));
                    }
                } else {
                    UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
                    UIUtil.showInfoSnackbar(getRootView(), getString(R.string.internal_error));
                }
                break;

            case R.id.loader_id_parse_public_keys:
                this.isParsePublicKeysNow = false;
                this.messagePartPgpPublicKeyList = (List<MessagePartPgpPublicKey>) result;
                displayPublicKeysViewIfCan(messagePartPgpPublicKeyList);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEARCH:
                UIUtil.hideSoftInput(AddContactActivity.this, v);

                if (GeneralUtil.isInternetConnectionAvailable(this)) {
                    getSupportLoaderManager().restartLoader(R.id.loader_id_search_public_key, null,
                            AddContactActivity.this);
                } else {
                    showInfoSnackbar(getRootView(), getString(R.string.internet_connection_is_not_available));
                }
                break;
        }

        return false;
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_search_public_key:
            case R.id.loader_id_parse_public_keys:
                UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
                Toast.makeText(this, TextUtils.isEmpty(e.getMessage())
                        ? getString(R.string.unknown_error) : e.getMessage(), Toast.LENGTH_SHORT).show();
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    @Override
    protected void initViews() {
        super.initViews();

        this.layoutPublicKeysContainer = findViewById(R.id.layoutPublicKeysContainer);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        this.buttonImportAll = findViewById(R.id.buttonImportAll);
        this.buttonImportAll.setOnClickListener(this);
        this.recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        this.recyclerViewContacts.setHasFixedSize(true);
        this.recyclerViewContacts.setLayoutManager(layoutManager);

        this.editTextEmailOrId = findViewById(R.id.editTextKeyIdOrEmail);
        this.editTextEmailOrId.setOnEditorActionListener(this);
    }

    private void displayPublicKeysViewIfCan(List<MessagePartPgpPublicKey> messagePartPgpPublicKeys) {
        if (messagePartPgpPublicKeys.isEmpty()) {
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
            Toast.makeText(this, R.string.no_public_key_found, Toast.LENGTH_SHORT).show();
        } else {
            this.recyclerViewContacts.setAdapter(new ImportPgpContactsRecyclerViewAdapter(messagePartPgpPublicKeys));
            this.buttonImportAll.setVisibility(messagePartPgpPublicKeys.size() > 1 ? View.VISIBLE : View.GONE);
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutContentView,
                    layoutPublicKeysContainer);
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress,
                    layoutPublicKeysContainer);
        }
    }
}
