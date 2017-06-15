/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply.
  * See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.util.DateTimeUtil;

import java.util.List;

/**
 * The MessageListAdapter responsible for displaying the message in the list.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 10:29
 *         E-mail: DenBond7@gmail.com
 */

public class MessageListAdapter extends BaseAdapter {
    private List<GeneralMessageDetails> generalMessageDetailses;
    private Context context;

    public MessageListAdapter(Context context, List<GeneralMessageDetails>
            generalMessageDetailses) {
        this.context = context;
        this.generalMessageDetailses = generalMessageDetailses;
    }

    @Override
    public int getCount() {
        return generalMessageDetailses.size();
    }

    @Override
    public GeneralMessageDetails getItem(int position) {
        return position < getCount() ? generalMessageDetailses.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GeneralMessageDetails generalMessageDetails = getItem(position);
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.messages_list_item,
                    parent, false);
            viewHolder.textViewSenderAddress =
                    (TextView) convertView.findViewById(R.id.textViewSenderAddress);
            viewHolder.textViewDate = (TextView) convertView.findViewById(R.id.textViewDate);
            viewHolder.textViewSubject = (TextView) convertView.findViewById(R.id.textViewSubject);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (viewHolder != null) {
            updateItem(generalMessageDetails, viewHolder);
        }

        return convertView;
    }

    /**
     * Remove an exists item from the emails list.
     *
     * @param generalMessageDetails The item which will be removed.
     */
    public void removeItem(GeneralMessageDetails generalMessageDetails) {
        if (generalMessageDetails != null) {
            int idOfDeleteCandidate = -1;

            for (GeneralMessageDetails messageDetails : generalMessageDetailses) {
                if (messageDetails.getUid() == generalMessageDetails.getUid()) {
                    idOfDeleteCandidate = generalMessageDetailses.indexOf(messageDetails);
                }
            }

            if (idOfDeleteCandidate != -1) {
                generalMessageDetailses.remove(idOfDeleteCandidate);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * Change the seen status of an exist item in the emails list.
     *
     * @param generalMessageDetails The item which the seen status will be changed.
     * @param isMessageSeen         true - if the message status already is seen, false otherwise.
     */
    public void changeMessageSeenState(GeneralMessageDetails generalMessageDetails, boolean
            isMessageSeen) {
        if (generalMessageDetails != null) {
            for (GeneralMessageDetails messageDetails : generalMessageDetailses) {
                if (messageDetails.getUid() == generalMessageDetails.getUid()) {
                    messageDetails.setSeen(isMessageSeen);
                    notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    /**
     * Update an information of some item.
     *
     * @param generalMessageDetails A model which consist information about the
     *                              generalMessageDetails.
     * @param viewHolder            A View holder object which consist links to views.
     */
    private void updateItem(GeneralMessageDetails generalMessageDetails, @NonNull ViewHolder
            viewHolder) {
        if (generalMessageDetails != null) {
            viewHolder.textViewSenderAddress.setText(generalMessageDetails.getFrom());
            viewHolder.textViewSubject.setText(generalMessageDetails.getSubject());
            viewHolder.textViewDate.setText(DateTimeUtil.formatSameDayTime(context,
                    generalMessageDetails.getReceiveDate().getTime()));

            if (generalMessageDetails.isSeen()) {
                changeViewsTypeface(viewHolder, Typeface.NORMAL);

                viewHolder.textViewSenderAddress.setTextColor(getColor(R.color.scorpion));
                viewHolder.textViewSubject.setTextColor(getColor(R.color.gray));
                viewHolder.textViewDate.setTextColor(getColor(R.color.gray));
            } else {
                changeViewsTypeface(viewHolder, Typeface.BOLD);

                viewHolder.textViewSenderAddress.setTextColor(getColor(android.R.color.black));
                viewHolder.textViewSubject.setTextColor(getColor(android.R.color.black));
                viewHolder.textViewDate.setTextColor(getColor(android.R.color.black));
            }
        } else {
            clearItem(viewHolder);
        }
    }

    private void changeViewsTypeface(@NonNull ViewHolder viewHolder, int typeface) {
        viewHolder.textViewSenderAddress.setTypeface(null, typeface);
        viewHolder.textViewSubject.setTypeface(null, typeface);
        viewHolder.textViewDate.setTypeface(null, typeface);
    }

    @SuppressWarnings("deprecation")
    private int getColor(int colorResourcesId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor
                    (colorResourcesId, context.getTheme());
        } else {
            return context.getResources().getColor(colorResourcesId);
        }
    }

    /**
     * Clear all views in the item.
     *
     * @param viewHolder A View holder object which consist links to views.
     */
    private void clearItem(@NonNull ViewHolder viewHolder) {
        viewHolder.textViewSenderAddress.setText(null);
        viewHolder.textViewSubject.setText(null);
        viewHolder.textViewDate.setText(null);

        changeViewsTypeface(viewHolder, Typeface.NORMAL);
    }

    /**
     * A view holder class which describes an information about item views.
     */
    private static class ViewHolder {
        TextView textViewSenderAddress;
        TextView textViewDate;
        TextView textViewSubject;
    }
}
