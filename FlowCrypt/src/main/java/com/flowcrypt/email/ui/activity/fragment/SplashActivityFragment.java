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
import com.flowcrypt.email.ui.activity.EmailManagerActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;

/**
 * This fragment containing a splash view. Also in this fragment, the implementation of Sign In
 * functions.
 */
public class SplashActivityFragment extends Fragment implements GoogleApiClient
        .OnConnectionFailedListener, View.OnClickListener {

    private static final String SCOPE_MAIL_GOOGLE_COM = "https://mail.google.com/";
    private static final int REQUEST_CODE_SIGN_IN = 100;
    /**
     * The main entry point for Google Play services integration.
     */
    private GoogleApiClient mGoogleApiClient;

    public SplashActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder
                (GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(SCOPE_MAIL_GOOGLE_COM))
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
        initViews(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkGoogleSignResult();
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
     * In this method we init all used views.
     */
    private void initViews(View view) {
        if (view.findViewById(R.id.buttonSignInWithGmail) != null) {
            view.findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonSignOut) != null) {
            view.findViewById(R.id.buttonSignOut).setOnClickListener(this);
        }
    }

    /**
     * In this method we check GoogleSignResult. If the user's cached credentials are valid the
     * OptionalPendingResult will be "done" and the GoogleSignInResult will be available
     * instantly. If the user has not previously signed in on this device or the sign-in has
     * expired, this asynchronous branch will attempt to sign in the user silently. Cross-device
     * single sign-on will occur in this branch.
     */
    private void checkGoogleSignResult() {
        OptionalPendingResult<GoogleSignInResult> optionalPendingResult = Auth.GoogleSignInApi
                .silentSignIn(mGoogleApiClient);
        if (optionalPendingResult.isDone()) {
            GoogleSignInResult googleSignInResult = optionalPendingResult.get();
            handleSignInResult(googleSignInResult);
        } else {
            optionalPendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    /**
     * Handle a Google sign result. In this method, we check the result received from Google.
     *
     * @param googleSignInResult Object which contain information about sign in.
     */
    private void handleSignInResult(final GoogleSignInResult googleSignInResult) {
        if (googleSignInResult.isSuccess()) {
            startActivity(new Intent(getActivity(), EmailManagerActivity.class));
        }
    }

    /**
     * Run a sign in intent.
     */
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }
}
