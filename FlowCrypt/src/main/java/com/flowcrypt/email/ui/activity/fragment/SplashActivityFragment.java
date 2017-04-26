package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * This fragment containing a splash view.
 */
public class SplashActivityFragment extends Fragment implements GoogleApiClient
        .OnConnectionFailedListener, View.OnClickListener {

    private static final int REQUEST_CODE_SIGN_IN = 100;

    private GoogleApiClient mGoogleApiClient;

    public SplashActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder
                (GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (view.findViewById(R.id.buttonSignInWithGmail) != null) {
            view.findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult =
                        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(googleSignInResult);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        UIUtil.showInfoSnackbar(getView(), connectionResult.getErrorMessage());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSignInWithGmail:
                if (GeneralUtil.isInternetConnectionAvailable(getActivity())) {
                    signIn();
                } else {
                    UIUtil.showInfoSnackbar(getView(),
                            getString(R.string.internet_connection_is_not_available));
                }
                break;
        }
    }

    /**
     * Handle a Google sign result. In this method, we check the result received from Google.
     *
     * @param googleSignInResult
     */
    private void handleSignInResult(GoogleSignInResult googleSignInResult) {
        if (googleSignInResult.isSuccess()) {
            // Todo-DenBond7 Need to handle a complete authentication.
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
            }
        } else {
            // Signed out, show unauthenticated UI.
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }
}
