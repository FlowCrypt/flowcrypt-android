/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.SplashActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;

/**
 * This fragment describes a logic for create or import private keys.
 *
 * @author DenBond7
 *         Date: 23.05.2017
 *         Time: 13:00
 *         E-mail: DenBond7@gmail.com
 */

public class CreateOrImportKeyFragment extends BaseFragment implements View.OnClickListener {
    private boolean isShowAnotherAccountButton = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra
                (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON)) {
            this.isShowAnotherAccountButton = getActivity().getIntent().getBooleanExtra
                    (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_or_import_key, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportMyKey:
                startActivity(new Intent(getContext(), ImportPrivateKeyActivity.class));
                break;

            case R.id.buttonSelectAnotherAccount:
                getActivity().finish();
                startActivity(SplashActivity.getSignOutIntent(getContext()));
                break;
        }
    }

    private void initViews(View view) {
        if (view.findViewById(R.id.buttonCreateNewKey) != null) {
            view.findViewById(R.id.buttonCreateNewKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonImportMyKey) != null) {
            view.findViewById(R.id.buttonImportMyKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonSelectAnotherAccount) != null) {
            if (isShowAnotherAccountButton) {
                view.findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
            } else {
                view.findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.GONE);
            }
        }
    }
}
