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
import com.flowcrypt.email.model.NoPgpFoundDialogAction;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter will be used in the {@link NoPgpFoundDialogFragment}
 *
 * @author Denis Bondarenko
 *         Date: 01.08.2017
 *         Time: 11:29
 *         E-mail: DenBond7@gmail.com
 */

public class NoPgpFoundDialogAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<NoPgpFoundDialogAction> noPgpFoundDialogActions;

    public NoPgpFoundDialogAdapter(Context context,
                                   List<NoPgpFoundDialogAction> noPgpFoundDialogActions) {
        this.noPgpFoundDialogActions = noPgpFoundDialogActions;
        this.inflater = LayoutInflater.from(context);

        if (this.noPgpFoundDialogActions == null) {
            this.noPgpFoundDialogActions = new ArrayList<>();
        }
    }

    @Override
    public int getCount() {
        return noPgpFoundDialogActions.size();
    }

    @Override
    public NoPgpFoundDialogAction getItem(int position) {
        return noPgpFoundDialogActions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NoPgpFoundDialogAction noPgpFoundDialogAction = getItem(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.no_pgp_found_dialog_item, parent, false);

            viewHolder.textViewItemTitle = (TextView) convertView.findViewById(R.id
                    .textViewNoPgpFoundDialogItem);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textViewItemTitle.setText(noPgpFoundDialogAction.getTitle());
        viewHolder.textViewItemTitle.setCompoundDrawablesWithIntrinsicBounds(
                noPgpFoundDialogAction.getIconResourceId(), 0, 0, 0);


        return convertView;
    }

    private class ViewHolder {
        TextView textViewItemTitle;
    }
}
