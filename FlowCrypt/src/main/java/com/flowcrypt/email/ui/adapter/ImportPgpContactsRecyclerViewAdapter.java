/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.PublicKeyInfo;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
  private List<PublicKeyInfo> publicKeys;

  public ImportPgpContactsRecyclerViewAdapter(List<PublicKeyInfo> publicKeys) {
    this.publicKeys = publicKeys;
  }

  @Override
  @NonNull
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.import_pgp_contact_item,
        parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
    final Context context = viewHolder.itemView.getContext();
    final PublicKeyInfo publicKeyInfo = publicKeys.get(position);

    viewHolder.buttonUpdateContact.setVisibility(View.GONE);
    viewHolder.buttonSaveContact.setVisibility(View.GONE);

    if (!TextUtils.isEmpty(publicKeyInfo.getKeyOwner())) {
      viewHolder.textViewKeyOwnerTemplate.setText(context.getString(R.string.template_message_part_public_key_owner,
          publicKeyInfo.getKeyOwner()));
    }

    UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_key_words,
        publicKeyInfo.getKeyWords()), viewHolder.textViewKeyWordsTemplate);
    UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", publicKeyInfo.getFingerprint(), 4)), viewHolder.textViewFingerprintTemplate);

    if (publicKeyInfo.hasPgpContact()) {
      if (publicKeyInfo.isUpdateEnabled()) {
        viewHolder.textViewAlreadyImported.setVisibility(View.GONE);
        viewHolder.buttonUpdateContact.setVisibility(View.VISIBLE);
        viewHolder.buttonUpdateContact.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            updateContact(viewHolder.getAdapterPosition(), v, context, publicKeyInfo);
          }
        });
      } else {
        viewHolder.textViewAlreadyImported.setVisibility(View.VISIBLE);
      }
    } else {
      viewHolder.textViewAlreadyImported.setVisibility(View.GONE);
      viewHolder.buttonSaveContact.setVisibility(View.VISIBLE);
      viewHolder.buttonSaveContact.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          saveContact(viewHolder.getAdapterPosition(), v, context, publicKeyInfo);
        }
      });
    }
  }

  @Override
  public int getItemCount() {
    return publicKeys != null ? publicKeys.size() : 0;
  }

  private void saveContact(int position, View v, Context context, PublicKeyInfo publicKeyInfo) {
    PgpContact pgpContact = new PgpContact(publicKeyInfo.getKeyOwner(), null, publicKeyInfo.getPublicKey(), true,
        null, false, publicKeyInfo.getFingerprint(), publicKeyInfo.getLongId(), publicKeyInfo.getKeyWords(), 0);

    Uri uri = new ContactsDaoSource().addRow(context, pgpContact);
    if (uri != null) {
      Toast.makeText(context, R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show();
      v.setVisibility(View.GONE);
      publicKeyInfo.setPgpContact(pgpContact);
      notifyItemChanged(position);
    } else {
      Toast.makeText(context, R.string.error_occurred_while_saving_contact, Toast.LENGTH_SHORT).show();
    }
  }

  private void updateContact(int position, View v, Context context, PublicKeyInfo publicKeyInfo) {
    PgpContact pgpContact = new PgpContact(publicKeyInfo.getKeyOwner(), null, publicKeyInfo.getPublicKey(), true,
        null, false, publicKeyInfo.getFingerprint(), publicKeyInfo.getLongId(), publicKeyInfo.getKeyWords(), 0);

    boolean isUpdated = new ContactsDaoSource().updatePgpContact(context, pgpContact) > 0;
    if (isUpdated) {
      Toast.makeText(context, R.string.contact_successfully_updated, Toast.LENGTH_SHORT).show();
      v.setVisibility(View.GONE);
      publicKeyInfo.setPgpContact(pgpContact);
      notifyItemChanged(position);
    } else {
      Toast.makeText(context, R.string.error_occurred_while_updating_contact, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * The view holder implementation for a better performance.
   */
  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView textViewKeyOwnerTemplate;
    TextView textViewKeyWordsTemplate;
    TextView textViewFingerprintTemplate;
    TextView textViewAlreadyImported;
    Button buttonSaveContact;
    Button buttonUpdateContact;

    ViewHolder(View itemView) {
      super(itemView);
      textViewKeyOwnerTemplate = itemView.findViewById(R.id.textViewKeyOwnerTemplate);
      textViewKeyWordsTemplate = itemView.findViewById(R.id.textViewKeyWordsTemplate);
      textViewFingerprintTemplate = itemView.findViewById(R.id.textViewFingerprintTemplate);
      textViewAlreadyImported = itemView.findViewById(R.id.textViewAlreadyImported);
      buttonSaveContact = itemView.findViewById(R.id.buttonSaveContact);
      buttonUpdateContact = itemView.findViewById(R.id.buttonUpdateContact);
    }
  }
}
