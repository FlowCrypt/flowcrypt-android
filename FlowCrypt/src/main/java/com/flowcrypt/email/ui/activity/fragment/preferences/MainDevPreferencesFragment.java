/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;

/**
 * The main developer options fragment.
 *
 * @author Denis Bondarenko
 *         Date: 10.07.2017
 *         Time: 11:19
 *         E-mail: DenBond7@gmail.com
 */
public class MainDevPreferencesFragment extends BaseDevPreferencesFragment implements
        SharedPreferences
                .OnSharedPreferenceChangeListener {
    private static final int REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS = 100;

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        addPreferencesFromResource(R.xml.dev_preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (sharedPreferences.getBoolean(key, false)) {
                        if (ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED) {
                            showApplicationDetailsSettingsActivity();
                        } else {
                            requestPermissions(new String[]{Manifest.permission
                                            .WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS);
                        }
                    } else {
                        showApplicationDetailsSettingsActivity();
                    }
                } else {
                    showApplicationDetailsSettingsActivity();
                }
                break;

            case Constants.PREFERENCES_KEY_IS_DETECT_MEMORY_LEAK_ENABLE:
            case Constants.PREFERENCES_KEY_IS_ACRA_ENABLE:
            case Constants.PREFERENCES_KEY_IS_MAIL_DEBUG_ENABLE:
                showApplicationDetailsSettingsActivity();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS:
                if (grantResults.length == 1 && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    showApplicationDetailsSettingsActivity();
                } else {
                    Toast.makeText(getActivity(), "Access not granted to write logs!!!", Toast
                            .LENGTH_SHORT).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showApplicationDetailsSettingsActivity() {
        Toast.makeText(getActivity(), R.string.toast_message_press_force_stop_to_apply_changes,
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
