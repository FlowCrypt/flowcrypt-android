/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment containing a splash view. Also in this fragment, the implementation of Sign In
 * functions.
 */
public class SplashActivityFragment extends Fragment implements View.OnClickListener {

    private OnSignInButtonClickListener onSignInButtonClickListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSignInButtonClickListener) {
            onSignInButtonClickListener = (OnSignInButtonClickListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnSignInButtonClickListener.class.getSimpleName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSignInWithGmail:
                if (GeneralUtil.isInternetConnectionAvailable(getActivity())) {
                    signInButtonWasClicked(SignInType.GMAIL);
                } else {
                    UIUtil.showInfoSnackbar(getView(),
                            getString(R.string.internet_connection_is_not_available));
                }
                break;
        }
    }

    /**
     * In this method we init all used views.
     */
    private void initViews(View view) {
        if (view.findViewById(R.id.buttonSignInWithGmail) != null) {
            view.findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
        }
    }

    /**
     * Handle a sign in button click.
     */
    private void signInButtonWasClicked(SignInType signInType) {
        if (onSignInButtonClickListener != null) {
            onSignInButtonClickListener.onSignInButtonClick(signInType);
        }
    }

    /**
     * Listener for sign in buttons.
     */
    public interface OnSignInButtonClickListener {
        void onSignInButtonClick(SignInType signInType);
    }
}
