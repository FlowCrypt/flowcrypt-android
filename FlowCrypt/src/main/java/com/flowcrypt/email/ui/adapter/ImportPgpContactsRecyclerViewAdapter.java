/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

/**
 * This adapter can be used for showing information about {@link PgpContact}s when we want to import them
 *
 * @author Denis Bondarenko
 * Date: 09.05.2018
 * Time: 08:07
 * E-mail: DenBond7@gmail.com
 */
public class ImportPgpContactsRecyclerViewAdapter extends
        RecyclerView.Adapter<ImportPgpContactsRecyclerViewAdapter.ViewHolder> {
    private List<MessagePartPgpPublicKey> messagePartPgpPublicKeyList;

    public ImportPgpContactsRecyclerViewAdapter(List<MessagePartPgpPublicKey> messagePartPgpPublicKeyList) {
        this.messagePartPgpPublicKeyList = messagePartPgpPublicKeyList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_part_public_key,
                parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Context context = viewHolder.itemView.getContext();
        MessagePartPgpPublicKey messagePartPgpPublicKey = messagePartPgpPublicKeyList.get(position);
        viewHolder.switchShowPublicKey.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(messagePartPgpPublicKey.getKeyOwner())) {
            viewHolder.textViewKeyOwnerTemplate.setText(
                    context.getString(R.string.template_message_part_public_key_owner,
                            messagePartPgpPublicKey.getKeyOwner()));
        }

        UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_key_words,
                messagePartPgpPublicKey.getKeyWords()), viewHolder.textViewKeyWordsTemplate);

        UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_fingerprint,
                GeneralUtil.doSectionsInText(" ", messagePartPgpPublicKey.getFingerprint(), 4)),
                viewHolder.textViewFingerprintTemplate);

        viewHolder.textViewPgpPublicKey.setText(messagePartPgpPublicKey.getValue());
        viewHolder.buttonUpdateContact.setVisibility(View.GONE);
        viewHolder.buttonSaveContact.setVisibility(View.GONE);

        if (messagePartPgpPublicKey.isPgpContactExists()) {
            if (messagePartPgpPublicKey.isPgpContactCanBeUpdated()) {
                viewHolder.buttonUpdateContact.setVisibility(View.VISIBLE);
                viewHolder.buttonUpdateContact.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        } else {
            viewHolder.buttonSaveContact.setVisibility(View.VISIBLE);
            viewHolder.buttonSaveContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messagePartPgpPublicKeyList != null ? messagePartPgpPublicKeyList.size() : 0;
    }

    /**
     * The view holder implementation for a better performance.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewKeyOwnerTemplate;
        TextView textViewKeyWordsTemplate;
        TextView textViewFingerprintTemplate;
        TextView textViewPgpPublicKey;
        Switch switchShowPublicKey;
        Button buttonSaveContact;
        Button buttonUpdateContact;

        ViewHolder(View itemView) {
            super(itemView);
            textViewKeyOwnerTemplate = itemView.findViewById(R.id.textViewKeyOwnerTemplate);
            textViewKeyWordsTemplate = itemView.findViewById(R.id.textViewKeyWordsTemplate);
            textViewFingerprintTemplate = itemView.findViewById(R.id.textViewFingerprintTemplate);
            textViewPgpPublicKey = itemView.findViewById(R.id.textViewPgpPublicKey);
            switchShowPublicKey = itemView.findViewById(R.id.switchShowPublicKey);
            buttonSaveContact = itemView.findViewById(R.id.buttonSaveContact);
            buttonUpdateContact = itemView.findViewById(R.id.buttonUpdateContact);
        }
    }
}
