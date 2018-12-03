/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter can be used to show info about public keys from the https://attester.flowcrypt.com/lookup/email/.
 *
 * @author Denis Bondarenko
 * Date: 14.11.2017
 * Time: 9:42
 * E-mail: DenBond7@gmail.com
 */

public class AttesterKeyAdapter extends BaseAdapter {
  private List<LookUpEmailResponse> lookUpEmailResponses;
  private List<String> keysLongIds;

  public AttesterKeyAdapter(Context context, List<LookUpEmailResponse> lookUpEmailResponses) {
    this.lookUpEmailResponses = lookUpEmailResponses;

    if (this.lookUpEmailResponses == null) {
      this.lookUpEmailResponses = new ArrayList<>();
    }

    this.keysLongIds = new KeysDaoSource().getAllKeysLongIds(context);
  }

  @Override
  public int getCount() {
    return lookUpEmailResponses.size();
  }

  @Override
  public LookUpEmailResponse getItem(int position) {
    return lookUpEmailResponses.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LookUpEmailResponse lookUpEmailResponse = getItem(position);
    Context context = parent.getContext();
    AttesterKeyAdapter.ViewHolder viewHolder;
    if (convertView == null) {
      viewHolder = new AttesterKeyAdapter.ViewHolder();
      convertView = LayoutInflater.from(context).inflate(R.layout.attester_key_item, parent, false);

      viewHolder.textViewKeyOwner = convertView.findViewById(R.id.textViewKeyOwner);
      viewHolder.textViewKeyAttesterStatus = convertView.findViewById(R.id.textViewKeyAttesterStatus);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (AttesterKeyAdapter.ViewHolder) convertView.getTag();
    }

    viewHolder.textViewKeyOwner.setText(lookUpEmailResponse.getEmail());

    if (TextUtils.isEmpty(lookUpEmailResponse.getPubKey())) {
      viewHolder.textViewKeyAttesterStatus.setText(R.string.no_public_key_recorded);
      viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.orange));
    } else if (isMatchedPublicKey(lookUpEmailResponse)) {
      viewHolder.textViewKeyAttesterStatus.setText(R.string.submitted_can_receive_encrypted_email);
      viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.colorPrimary));
    } else {
      viewHolder.textViewKeyAttesterStatus.setText(R.string.wrong_public_key_recorded);
      viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.red));
    }

    return convertView;
  }

  /**
   * Check is public key found, and the longid does not match any longids of saved keys.
   *
   * @param lookUpEmailResponse The {@link LookUpEmailResponse} object which contains info about a public key from
   *                            the Attester API.
   * @return true if public key found, and the longid does not match any longids of saved keys, otherwise false.
   */
  private boolean isMatchedPublicKey(LookUpEmailResponse lookUpEmailResponse) {

    for (String longId : keysLongIds) {
      if (longId.equals(lookUpEmailResponse.getLongId())) {
        return true;
      }
    }

    return false;
  }

  /**
   * The view holder implementation for a better performance.
   */
  private static class ViewHolder {
    TextView textViewKeyOwner;
    TextView textViewKeyAttesterStatus;
  }
}
