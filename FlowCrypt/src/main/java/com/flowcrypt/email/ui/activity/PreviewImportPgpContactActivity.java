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
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.PublicKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter;
import com.flowcrypt.email.ui.loader.ParsePublicKeysFromStringAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 *         Date: 10.05.2018
 *         Time: 18:01
 *         E-mail: DenBond7@gmail.com
 */
public class PreviewImportPgpContactActivity extends BaseBackStackActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult> {
    public static final String KEY_EXTRA_PUBLIC_KEY_STRING
            = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEY_STRING", PreviewImportPgpContactActivity.class);

    private RecyclerView recyclerViewContacts;
    private TextView buttonImportAll;
    private View layoutContentView;
    private View layoutProgress;

    private String publicKeysString;
    private List<PublicKeyInfo> publicKeyInfoList;
    private View emptyView;

    public static Intent newIntent(Context context, String publicKeysString) {
        Intent intent = new Intent(context, PreviewImportPgpContactActivity.class);
        intent.putExtra(KEY_EXTRA_PUBLIC_KEY_STRING, publicKeysString);
        return intent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_preview_import_pgp_contact;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().hasExtra(KEY_EXTRA_PUBLIC_KEY_STRING)) {
            initViews();

            publicKeysString = getIntent().getStringExtra(KEY_EXTRA_PUBLIC_KEY_STRING);
            if (TextUtils.isEmpty(publicKeysString)) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            } else {
                getSupportLoaderManager().initLoader(R.id.loader_id_parse_public_keys, null, this);
            }
        } else {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_preview_import_pgp_contact, menu);
        return true;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportAll:
                ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
                List<PgpContact> newCandidates = new ArrayList<>();
                List<PgpContact> updateCandidates = new ArrayList<>();

                for (PublicKeyInfo publicKeyInfo : publicKeyInfoList) {
                    PgpContact pgpContact = new PgpContact(publicKeyInfo.getKeyOwner(),
                            null,
                            publicKeyInfo.getPublicKey(),
                            true,
                            null,
                            false,
                            publicKeyInfo.getFingerprint(),
                            publicKeyInfo.getLongId(),
                            publicKeyInfo.getKeyWords(), 0);

                    if (publicKeyInfo.isPgpContactExists()) {
                        if (publicKeyInfo.isPgpContactCanBeUpdated()) {
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
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_parse_public_keys:
                UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
                return new ParsePublicKeysFromStringAsyncTaskLoader(this, publicKeysString);

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

    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_parse_public_keys:
                publicKeyInfoList = (List<PublicKeyInfo>) result;
                if (!publicKeyInfoList.isEmpty()) {
                    UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
                    recyclerViewContacts.setAdapter(new ImportPgpContactsRecyclerViewAdapter(publicKeyInfoList));
                    buttonImportAll.setVisibility(publicKeyInfoList.size() > 1 ? View.VISIBLE : View.GONE);
                } else {
                    UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, emptyView);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_parse_public_keys:
                setResult(Activity.RESULT_CANCELED);
                Toast.makeText(this, TextUtils.isEmpty(e.getMessage())
                        ? getString(R.string.unknown_error) : e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    private void initViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutContentView = findViewById(R.id.layoutContentView);
        layoutProgress = findViewById(R.id.layoutProgress);
        buttonImportAll = findViewById(R.id.buttonImportAll);
        buttonImportAll.setOnClickListener(this);
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        recyclerViewContacts.setHasFixedSize(true);
        recyclerViewContacts.setLayoutManager(layoutManager);
        emptyView = findViewById(R.id.emptyView);
    }
}
