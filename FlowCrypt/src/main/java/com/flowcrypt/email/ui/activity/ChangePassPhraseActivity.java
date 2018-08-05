/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity;
import com.flowcrypt.email.util.UIUtil;

/**
 * This activity describes a logic of changing the pass phrase of all imported private keys of an active account.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseActivity extends BasePassPhraseManagerActivity {

    public static Intent newIntent(Context context, AccountDao accountDao) {
        Intent intent = new Intent(context, ChangePassPhraseActivity.class);
        intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, accountDao);
        return intent;
    }

    @Override
    public void onConfirmPassPhraseSuccess() {
        layoutSecondPasswordCheck.setVisibility(View.GONE);
        layoutSuccess.setVisibility(View.VISIBLE);
        UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSuccess:
                finish();
                break;

            default:
                super.onClick(v);
        }
    }

    @Override
    protected void initViews() {
        super.initViews();

        textViewFirstPasswordCheckTitle.setText(R.string.changing_pass_phrase);
        textViewSecondPasswordCheckTitle.setText(R.string.changing_pass_phrase);

        textViewSuccessTitle.setText(R.string.done);
        textViewSuccessSubTitle.setText(R.string.pass_phrase_changed);
        buttonSuccess.setText(R.string.back);
    }
}
