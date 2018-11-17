/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;


/**
 * A custom {@link androidx.preference.Preference}
 *
 * @author Denis Bondarenko
 * Date: 17.11.2018
 * Time: 13:00
 * E-mail: DenBond7@gmail.com
 */
public class BuildConfInfoPreference extends DialogPreference {
  public BuildConfInfoPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public BuildConfInfoPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public BuildConfInfoPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BuildConfInfoPreference(Context context) {
    super(context);
  }
}
