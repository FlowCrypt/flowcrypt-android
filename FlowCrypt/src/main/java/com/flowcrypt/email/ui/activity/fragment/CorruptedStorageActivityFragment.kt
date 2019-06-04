/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.CorruptedStorageActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * It's a root fragment of {@link CorruptedStorageActivity}
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
public class CorruptedStorageActivityFragment extends Fragment implements View.OnClickListener {

  public CorruptedStorageActivityFragment() {
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_corrupted_storage, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    TextView textViewHeader = view.findViewById(R.id.textViewHeader);
    textViewHeader.setText(getString(R.string.store_space_was_corrupted, getString(R.string.support_email)));

    TextView textViewFooter = view.findViewById(R.id.textViewFooter);
    textViewFooter.setText(getString(R.string.wipe_app_settings, getString(R.string.app_name)));

    View btnResetAppSettings = view.findViewById(R.id.btnResetAppSettings);
    if (btnResetAppSettings != null) {
      btnResetAppSettings.setOnClickListener(this);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnResetAppSettings:
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        break;
    }
  }
}
