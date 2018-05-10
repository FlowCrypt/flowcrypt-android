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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 10.05.2018
 * Time: 18:01
 * E-mail: DenBond7@gmail.com
 */
public class PreviewImportPgpContactActivity extends BaseBackStackActivity implements View.OnClickListener {
    public static final String KEY_EXTRA_LIST
            = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_LIST", PreviewImportPgpContactActivity.class);

    private ArrayList<MessagePartPgpPublicKey> messagePartPgpPublicKeyList;

    private RecyclerView recyclerViewContacts;
    private TextView buttonImportAll;

    public static Intent newIntent(Context context, ArrayList<MessagePartPgpPublicKey> messagePartPgpPublicKeyList) {
        Intent intent = new Intent(context, PreviewImportPgpContactActivity.class);
        intent.putParcelableArrayListExtra(KEY_EXTRA_LIST, messagePartPgpPublicKeyList);
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
        if (getIntent() != null && getIntent().hasExtra(KEY_EXTRA_LIST)) {
            this.messagePartPgpPublicKeyList = getIntent().getParcelableArrayListExtra(KEY_EXTRA_LIST);

            initViews();

            this.recyclerViewContacts.setAdapter(new ImportPgpContactsRecyclerViewAdapter(messagePartPgpPublicKeyList));
            this.buttonImportAll.setVisibility(messagePartPgpPublicKeyList.size() > 1 ? View.VISIBLE : View.GONE);
        } else {
            setResult(Activity.RESULT_CANCELED);
            finish();
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
        }
    }

    private void initViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        this.buttonImportAll = findViewById(R.id.buttonImportAll);
        this.buttonImportAll.setOnClickListener(this);
        this.recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        this.recyclerViewContacts.setHasFixedSize(true);
        this.recyclerViewContacts.setLayoutManager(layoutManager);
    }
}
