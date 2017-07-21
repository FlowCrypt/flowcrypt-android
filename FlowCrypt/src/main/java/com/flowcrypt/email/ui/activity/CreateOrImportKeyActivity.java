/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity describes a logic for create ot import private keys.
 *
 * @author DenBond7
 *         Date: 23.05.2017.
 *         Time: 16:15.
 *         E-mail: DenBond7@gmail.com
 */
public class CreateOrImportKeyActivity extends BaseActivity implements View.OnClickListener {
    private static final String KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON =
            GeneralUtil.generateUniqueExtraKey("KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON",
                    CreateOrImportKeyActivity.class);

    private static final String KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT",
            CreateOrImportKeyActivity.class);

    private boolean isShowAnotherAccountButton = true;
    private Account account;

    public static Intent newIntent(Context context, Account account, boolean isShowAnotherAccount) {
        Intent intent = new Intent(context, CreateOrImportKeyActivity.class);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, isShowAnotherAccount);
        return intent;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_create_or_import_key;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            this.isShowAnotherAccountButton = getIntent().getBooleanExtra
                    (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, true);
            this.account = getIntent().getParcelableExtra(CreateOrImportKeyActivity.KEY_ACCOUNT);
        }

        initViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportMyKey:
                startActivity(ImportPrivateKeyActivity.newIntent(this, account));
                break;

            case R.id.buttonSelectAnotherAccount:
                finish();
                startActivity(SplashActivity.getSignOutIntent(this));
                break;
        }
    }

    private void initViews() {
        if (findViewById(R.id.buttonCreateNewKey) != null) {
            findViewById(R.id.buttonCreateNewKey).setOnClickListener(this);
        }

        if (findViewById(R.id.buttonImportMyKey) != null) {
            findViewById(R.id.buttonImportMyKey).setOnClickListener(this);
        }

        if (findViewById(R.id.buttonSelectAnotherAccount) != null) {
            if (isShowAnotherAccountButton) {
                findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
            } else {
                findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.GONE);
            }
        }
    }
}
