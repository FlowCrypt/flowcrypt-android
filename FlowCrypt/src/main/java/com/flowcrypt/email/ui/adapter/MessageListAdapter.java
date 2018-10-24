/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.DateTimeUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;

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
    private Pattern patternSenderName;

    public MessageListAdapter(Context context, Cursor c) {
        super(context, c, false);
        this.messageDaoSource = new MessageDaoSource();
        this.patternSenderName = prepareSenderNamePattern();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.messages_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        GeneralMessageDetails generalMessageDetails = messageDaoSource.getMessageInfo(cursor);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.textViewSenderAddress = view.findViewById(R.id
                .textViewSenderAddress);
        viewHolder.textViewDate = view.findViewById(R.id.textViewDate);
        viewHolder.textViewSubject = view.findViewById(R.id.textViewSubject);
        viewHolder.imageViewAttachments = view.findViewById(R.id.imageViewAttachments);
        viewHolder.viewIsEncrypted = view.findViewById(R.id.viewIsEncrypted);

        updateItem(context, generalMessageDetails, viewHolder);
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            this.folderType = FoldersManager.getFolderTypeForImapFolder(folder);
        } else {
            folderType = null;
        }
    }

    /**
     * Update information of some item.
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

            if (folderType != null) {
                switch (folderType) {
                    case SENT:
                        viewHolder.textViewSenderAddress.setText(generateAddresses(generalMessageDetails.getTo()));
                        break;

                    case OUTBOX:
                        viewHolder.textViewSenderAddress.setText(generateOutboxStatus(viewHolder.textViewSenderAddress
                                .getContext(), generalMessageDetails.getMessageState()));
                        break;

                    default:
                        viewHolder.textViewSenderAddress.setText(generateAddresses(generalMessageDetails.getFrom()));
                        break;
                }
            } else {
                viewHolder.textViewSenderAddress.setText(generateAddresses(generalMessageDetails.getFrom()));
            }

            viewHolder.textViewSubject.setText(subject);
            if (folderType == FoldersManager.FolderType.OUTBOX) {
                viewHolder.textViewDate.setText(DateTimeUtil.formatSameDayTime(context,
                        generalMessageDetails.getSentDateInMillisecond()));
            } else {
                viewHolder.textViewDate.setText(DateTimeUtil.formatSameDayTime(context,
                        generalMessageDetails.getReceivedDateInMillisecond()));
            }

            if (generalMessageDetails.isSeen()) {
                changeViewsTypeface(viewHolder, Typeface.NORMAL);
                viewHolder.textViewSenderAddress.setTextColor(UIUtil.getColor(context, R.color.dark));
                viewHolder.textViewDate.setTextColor(UIUtil.getColor(context, R.color.gray));
            } else {
                changeViewsTypeface(viewHolder, Typeface.BOLD);
                viewHolder.textViewSenderAddress.setTextColor(UIUtil.getColor(context, android.R.color.black));
                viewHolder.textViewDate.setTextColor(UIUtil.getColor(context, android.R.color.black));
            }

            viewHolder.imageViewAttachments.setVisibility(generalMessageDetails
                    .isMessageHasAttachment() ? View.VISIBLE : View.GONE);
            viewHolder.viewIsEncrypted.setVisibility(generalMessageDetails.isEncrypted() ? View.VISIBLE : View.GONE);
        } else {
            clearItem(viewHolder);
        }
    }

    private void changeViewsTypeface(@NonNull ViewHolder viewHolder, int typeface) {
        viewHolder.textViewSenderAddress.setTypeface(null, typeface);
        viewHolder.textViewDate.setTypeface(null, typeface);
    }

    /**
     * Prepare a {@link Pattern} which will be used for finding some information in the sender name. This pattern is
     * case insensitive.
     *
     * @return A generated {@link Pattern}.
     */
    private Pattern prepareSenderNamePattern() {
        List<String> domains = new ArrayList<>();
        domains.add("gmail.com");
        domains.add("yahoo.com");
        domains.add("live.com");
        domains.add("outlook.com");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("@");
        stringBuilder.append("(");
        stringBuilder.append(domains.get(0));

        for (int i = 1; i < domains.size(); i++) {
            stringBuilder.append("|");
            stringBuilder.append(domains.get(i));
        }
        stringBuilder.append(")$");

        return Pattern.compile(stringBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Prepare the sender name.
     * <ul>
     * <li>Remove common mail domains: gmail.com, yahoo.com, live.com, outlook.com</li>
     * </ul>
     *
     * @param name An incoming name
     * @return A generated sender name.
     */
    private String prepareSenderName(String name) {
        return patternSenderName.matcher(name).replaceFirst("");
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
        viewHolder.imageViewAttachments.setVisibility(View.GONE);
        viewHolder.viewIsEncrypted.setVisibility(View.GONE);

        changeViewsTypeface(viewHolder, Typeface.NORMAL);
    }

    private String generateAddresses(InternetAddress[] internetAddresses) {
        if (internetAddresses == null)
            return "null";

        int iMax = internetAddresses.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            InternetAddress internetAddress = internetAddresses[i];
            String displayName = TextUtils.isEmpty(internetAddress.getPersonal()) ? internetAddress.getAddress() :
                    internetAddress.getPersonal();
            b.append(displayName);
            if (i == iMax)
                return prepareSenderName(b.toString());
            b.append(", ");
        }
    }

    private CharSequence generateOutboxStatus(Context context, MessageState messageState) {
        String me = context.getString(R.string.me);
        String state = "";
        int stateTextColor = ContextCompat.getColor(context, R.color.red);

        switch (messageState) {
            case NEW:
            case NEW_FORWARDED:
                state = context.getString(R.string.preparing);
                stateTextColor = ContextCompat.getColor(context, R.color.colorAccent);
                break;

            case QUEUED:
                state = context.getString(R.string.queued);
                stateTextColor = ContextCompat.getColor(context, R.color.colorAccent);
                break;

            case SENDING:
                state = context.getString(R.string.sending);
                stateTextColor = ContextCompat.getColor(context, R.color.colorPrimary);
                break;

            case ERROR_CACHE_PROBLEM:
            case ERROR_DURING_CREATION:
            case ERROR_ORIGINAL_MESSAGE_MISSING:
            case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
                stateTextColor = ContextCompat.getColor(context, R.color.red);

                switch (messageState) {
                    case ERROR_CACHE_PROBLEM:
                        state = context.getString(R.string.cache_error);
                        break;

                    case ERROR_DURING_CREATION:
                        state = context.getString(R.string.could_not_create);
                        break;

                    case ERROR_ORIGINAL_MESSAGE_MISSING:
                        state = context.getString(R.string.original_message_missing);
                        break;

                    case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
                        state = context.getString(R.string.original_attachment_not_found);
                        break;
                }

                break;
        }

        int meTextSize = context.getResources().getDimensionPixelSize(R.dimen.default_text_size_big);
        int statusTextSize = context.getResources().getDimensionPixelSize(R.dimen.default_text_size_very_small);

        SpannableString spannableStringMe = new SpannableString(me);
        spannableStringMe.setSpan(new AbsoluteSizeSpan(meTextSize), 0, me.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        SpannableString spannableStringStatus = new SpannableString(state);
        spannableStringStatus.setSpan(new AbsoluteSizeSpan(statusTextSize), 0, state.length(), Spanned
                .SPAN_INCLUSIVE_INCLUSIVE);
        spannableStringStatus.setSpan(new ForegroundColorSpan(stateTextColor), 0, state.length(), Spanned
                .SPAN_INCLUSIVE_INCLUSIVE);

        return TextUtils.concat(spannableStringMe, " ", spannableStringStatus);
    }

    /**
     * A view holder class which describes information about item views.
     */
    private static class ViewHolder {
        TextView textViewSenderAddress;
        TextView textViewDate;
        TextView textViewSubject;
        ImageView imageViewAttachments;
        View viewIsEncrypted;
    }
}
