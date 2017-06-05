package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;

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
    private java.text.DateFormat dateFormat;

    public MessageListAdapter(Context context, List<GeneralMessageDetails>
            generalMessageDetailses) {
        this.context = context;
        this.generalMessageDetailses = generalMessageDetailses;
        dateFormat = DateFormat.getTimeFormat(context);
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
            viewHolder.senderAddressTextView =
                    (TextView) convertView.findViewById(R.id.senderAddressTextView);
            viewHolder.dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
            viewHolder.messageTextView = (TextView) convertView.findViewById(R.id.subjectTextView);
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

            for (GeneralMessageDetails messageDetails :
                    generalMessageDetailses) {
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
     * Update an information of some item.
     *
     * @param generalMessageDetails A model which consist information about the
     *                              generalMessageDetails.
     * @param viewHolder            A View holder object which consist links to views.
     */
    private void updateItem(GeneralMessageDetails generalMessageDetails, @NonNull ViewHolder
            viewHolder) {
        if (generalMessageDetails != null) {
            viewHolder.senderAddressTextView.setText(generalMessageDetails.getFrom());
            viewHolder.messageTextView.setText(generalMessageDetails.getSubject());
            viewHolder.dateTextView.setText(dateFormat.format(generalMessageDetails
                    .getReceiveDate()));
        } else {
            clearItem(viewHolder);
        }
    }

    /**
     * Clear all views in the item.
     *
     * @param viewHolder A View holder object which consist links to views.
     */
    private void clearItem(@NonNull ViewHolder viewHolder) {
        viewHolder.senderAddressTextView.setText(null);
        viewHolder.messageTextView.setText(null);
        viewHolder.dateTextView.setText(null);
    }

    /**
     * A view holder class which describes an information about item views.
     */
    private static class ViewHolder {
        TextView senderAddressTextView;
        TextView dateTextView;
        TextView messageTextView;
    }
}
