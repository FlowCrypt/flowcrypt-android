/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.DateTimeUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * The MessageListAdapter responsible for displaying the message in the list.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 10:29
 *         E-mail: DenBond7@gmail.com
 */

public class MessageListAdapter extends CursorAdapter {
    private MessageDaoSource messageDaoSource;
    private Folder folder;
    private FoldersManager.FolderType folderType;

    public MessageListAdapter(Context context, Cursor c) {
        super(context, c, false);
        this.messageDaoSource = new MessageDaoSource();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.messages_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        GeneralMessageDetails generalMessageDetails = messageDaoSource.getMessageInfo(cursor);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.textViewSenderAddress = (TextView) view.findViewById(R.id
                .textViewSenderAddress);
        viewHolder.textViewDate = (TextView) view.findViewById(R.id.textViewDate);
        viewHolder.textViewSubject = (TextView) view.findViewById(R.id.textViewSubject);

        updateItem(context, generalMessageDetails, viewHolder);
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            this.folderType = FoldersManager.getFolderTypeForImapFodler(folder.getAttributes());
        } else {
            folderType = null;
        }
    }

    /**
     * Update an information of some item.
     *
     * @param generalMessageDetails A model which consist information about the
     *                              generalMessageDetails.
     * @param viewHolder            A View holder object which consist links to views.
     */
    private void updateItem(Context context, GeneralMessageDetails generalMessageDetails,
                            @NonNull ViewHolder viewHolder) {
        if (generalMessageDetails != null) {
            String subject = TextUtils.isEmpty(generalMessageDetails.getSubject()) ?
                    context.getString(R.string.no_subject) :
                    generalMessageDetails.getSubject();

            if (folderType == FoldersManager.FolderType.SENT) {
                viewHolder.textViewSenderAddress.setText(
                        generateAddresses(generalMessageDetails.getTo()));
            } else {
                viewHolder.textViewSenderAddress.setText(
                        generateAddresses(generalMessageDetails.getFrom()));
            }

            viewHolder.textViewSubject.setText(subject);
            viewHolder.textViewDate.setText(DateTimeUtil.formatSameDayTime(context,
                    generalMessageDetails.getReceivedDateInMillisecond()));

            if (generalMessageDetails.isSeen()) {
                changeViewsTypeface(viewHolder, Typeface.NORMAL);

                viewHolder.textViewSenderAddress.setTextColor(UIUtil.getColor(context, R.color
                        .scorpion));
                viewHolder.textViewSubject.setTextColor(UIUtil.getColor(context, R.color.gray));
                viewHolder.textViewDate.setTextColor(UIUtil.getColor(context, R.color.gray));
            } else {
                changeViewsTypeface(viewHolder, Typeface.BOLD);

                viewHolder.textViewSenderAddress.setTextColor(
                        UIUtil.getColor(context, android.R.color.black));
                viewHolder.textViewSubject.setTextColor(UIUtil.getColor(context, android.R.color
                        .black));
                viewHolder.textViewDate.setTextColor(UIUtil.getColor(context, android.R.color
                        .black));
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

    private String generateAddresses(String[] strings) {
        if (strings == null)
            return "null";

        int iMax = strings.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(String.valueOf(strings[i]));
            if (i == iMax)
                return b.toString();
            b.append(", ");
        }
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
