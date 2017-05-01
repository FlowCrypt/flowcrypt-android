package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.EmailManagerActivity;

/**
 * This class described restore an account functionality. There we can activate and save to the
 * security storage downloaded keys.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:40
 *         E-mail: DenBond7@gmail.com
 */
public class RestoreAccountActivityFragment extends Fragment {

    public RestoreAccountActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restore_account, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonLoadAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EmailManagerActivity.class));
            }
        });
    }
}
