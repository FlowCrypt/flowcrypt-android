/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.util.UIUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This is a custom realization of {@link ArrayAdapter} which can be used for showing the sender addresses.
 *
 * @author Denis Bondarenko
 * Date: 28.11.2018
 * Time: 5:22 PM
 * E-mail: DenBond7@gmail.com
 */
public class FromAddressesAdapter<T> extends ArrayAdapter<T> {
  private Map<String, Boolean> infoAboutKeysAvailable = new HashMap<>();
  private int originalColor;
  private boolean useKeysInfo;

  public FromAddressesAdapter(@NonNull Context context, int resource, int textViewResId, @NonNull List<T> objects) {
    super(context, resource, textViewResId, objects);
  }

  @Override
  public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    View view = super.getDropDownView(position, convertView, parent);

    TextView textView = view.findViewById(android.R.id.text1);

    if (textView != null) {
      textView.setTextColor(isEnabled(position) ? originalColor : UIUtil.getColor(getContext(), R.color.gray));
    }

    return view;
  }

  @Override
  public boolean isEnabled(int position) {
    if (useKeysInfo && getItem(position) instanceof String) {
      String email = (String) getItem(position);
      return infoAboutKeysAvailable.get(email);
    } else return super.isEnabled(position);
  }

  @Override
  public void setDropDownViewResource(int resource) {
    super.setDropDownViewResource(resource);
    TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(resource, null);
    originalColor = textView.getCurrentTextColor();
  }

  /**
   * This method can be used to disable the keys checking.
   *
   * @param useKeysInfo true if we want to check the key available, otherwise false.
   */
  public void setUseKeysInfo(boolean useKeysInfo) {
    this.useKeysInfo = useKeysInfo;
    notifyDataSetChanged();
  }

  /**
   * Update information about the key availability for the given email.
   *
   * @param emailAddress The given email address
   * @param hasPgp       true if we have a private key for the given email address, otherwise false
   */
  public void updateKeyAvailable(String emailAddress, boolean hasPgp) {
    if (infoAboutKeysAvailable != null) {
      infoAboutKeysAvailable.put(emailAddress, hasPgp);
    }
  }
}
