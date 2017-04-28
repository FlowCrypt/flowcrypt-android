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
import com.flowcrypt.email.model.SimpleMessageModel;

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
    private List<SimpleMessageModel> simpleMessageModels;
    private Context context;
    private java.text.DateFormat dateFormat;

    public MessageListAdapter(Context context, List<SimpleMessageModel> simpleMessageModels) {
        this.context = context;
        this.simpleMessageModels = simpleMessageModels;
        dateFormat = DateFormat.getTimeFormat(context);
    }

    @Override
    public int getCount() {
        return simpleMessageModels.size();
    }

    @Override
    public SimpleMessageModel getItem(int position) {
        return position < getCount() ? simpleMessageModels.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleMessageModel simpleMessageModel = getItem(position);
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
            updateItem(simpleMessageModel, viewHolder);
        }

        return convertView;
    }

    /**
     * Update an information of some item.
     *
     * @param simpleMessageModel A model which consist information about the message.
     * @param viewHolder         A View holder object which consist links to views.
     */
    private void updateItem(SimpleMessageModel simpleMessageModel, @NonNull ViewHolder viewHolder) {
        if (simpleMessageModel != null) {
            viewHolder.senderAddressTextView.setText(simpleMessageModel.getFrom());
            viewHolder.messageTextView.setText(simpleMessageModel.getSubject());
            viewHolder.dateTextView.setText(dateFormat.format(simpleMessageModel.getReceiveDate()));
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
