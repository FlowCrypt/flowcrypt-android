/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.os.Bundle;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.lang.reflect.Field;
import java.util.Formatter;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * This fragment shows a general information about the current build.
 *
 * @author Denis Bondarenko
 * Date: 10.07.2017
 * Time: 12:11
 * E-mail: DenBond7@gmail.com
 */
public class BuildConfigInfoPreferencesFragment extends PreferenceDialogFragmentCompat {

  private String msg;

  public static BuildConfigInfoPreferencesFragment newInstance(String key) {
    BuildConfigInfoPreferencesFragment fragment = new BuildConfigInfoPreferencesFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Class clazz = BuildConfig.class;
    Field[] fields = clazz.getDeclaredFields();

    Formatter formatter = new Formatter(Locale.getDefault());

    for (Field field : fields) {
      try {
        formatter.format("%s: %s\n\n", field.getName(), field.get(null));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
      }
    }

    msg = formatter.toString();
  }

  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    super.onPrepareDialogBuilder(builder);
    builder.setMessage(msg);
    builder.setNegativeButton(null, null);
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {

  }
}
