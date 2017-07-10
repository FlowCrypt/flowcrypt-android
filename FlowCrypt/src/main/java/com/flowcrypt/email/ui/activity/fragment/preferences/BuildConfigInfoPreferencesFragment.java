/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.flowcrypt.email.BuildConfig;

import java.lang.reflect.Field;
import java.util.Formatter;
import java.util.Locale;

/**
 * This fragment shows a general information about the current build.
 *
 * @author Denis Bondarenko
 *         Date: 10.07.2017
 *         Time: 12:11
 *         E-mail: DenBond7@gmail.com
 */
public class BuildConfigInfoPreferencesFragment extends DialogPreference {
    public BuildConfigInfoPreferencesFragment(Context context, AttributeSet attrs) {
        super(context, attrs);
        Class clazz = BuildConfig.class;
        Field[] fields = clazz.getDeclaredFields();

        Formatter formatter = new Formatter(Locale.getDefault());

        for (Field field : fields) {
            try {
                formatter.format("%s: %s\n\n", field.getName(), field.get(null));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        setDialogMessage(formatter.toString());
    }

    public BuildConfigInfoPreferencesFragment(Context context) {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(null, null);
    }
}
