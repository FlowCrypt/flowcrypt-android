/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.PrivateKeyModel;

import java.util.List;

/**
 * This adapter can be used for showing a list of the private keys.
 *
 * @author Denis Bondarenko
 *         Date: 01.12.2017
 *         Time: 12:44
 *         E-mail: DenBond7@gmail.com
 */

public class PrivateKeyAdapter extends BaseAdapter {
    private List<PrivateKeyModel> privateKeyModelList;
    private LayoutInflater layoutInflater;

    public PrivateKeyAdapter(Context context, List<PrivateKeyModel> privateKeyModelList) {
        this.privateKeyModelList = privateKeyModelList;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return privateKeyModelList != null ? privateKeyModelList.size() : 0;
    }

    @Override
    public PrivateKeyModel getItem(int position) {
        return privateKeyModelList != null ? privateKeyModelList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PrivateKeyModel privateKeyModel = getItem(position);

        PrivateKeyAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new PrivateKeyAdapter.ViewHolder();
            convertView = layoutInflater.inflate(R.layout.key_item, parent, false);

            viewHolder.textViewKeyOwner = convertView.findViewById(R.id.textViewKeyOwner);
            viewHolder.textViewKeywords = convertView.findViewById(R.id.textViewKeywords);
            viewHolder.textViewCreationDate = convertView.findViewById(R.id.textViewCreationDate);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (PrivateKeyAdapter.ViewHolder) convertView.getTag();
        }

        if (privateKeyModel != null) {
            viewHolder.textViewKeyOwner.setText(privateKeyModel.getKeyOwner());
            viewHolder.textViewKeywords.setText(privateKeyModel.getKeywords());
            viewHolder.textViewCreationDate.setText(privateKeyModel.getCreationDate());
        } else {
            viewHolder.textViewKeyOwner.setText(null);
            viewHolder.textViewKeywords.setText(null);
            viewHolder.textViewCreationDate.setText(null);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView textViewKeyOwner;
        TextView textViewKeywords;
        TextView textViewCreationDate;
    }
}
