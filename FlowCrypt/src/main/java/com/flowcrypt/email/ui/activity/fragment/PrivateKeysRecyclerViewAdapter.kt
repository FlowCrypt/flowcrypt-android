/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.model.PgpContact;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This adapter will be used to show a list of private keys.
 *
 * @author Denis Bondarenko
 * Date: 2/13/19
 * Time: 6:24 PM
 * E-mail: DenBond7@gmail.com
 */
public class PrivateKeysRecyclerViewAdapter extends RecyclerView.Adapter<PrivateKeysRecyclerViewAdapter.ViewHolder> {

  private final OnKeySelectedListener listener;
  private List<NodeKeyDetails> list;
  private java.text.DateFormat dateFormat;

  public PrivateKeysRecyclerViewAdapter(Context context, List<NodeKeyDetails> items, OnKeySelectedListener listener) {
    this.list = items;
    this.listener = listener;
    this.dateFormat = DateFormat.getMediumDateFormat(context);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.key_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int position) {
    final NodeKeyDetails nodeKeyDetails = list.get(position);
    if (nodeKeyDetails != null) {
      PgpContact pgpContact = nodeKeyDetails.getPrimaryPgpContact();

      viewHolder.textViewKeyOwner.setText(pgpContact.getEmail());
      viewHolder.textViewKeywords.setText(nodeKeyDetails.getKeywords());

      long timestamp = nodeKeyDetails.getCreated();
      if (timestamp != -1) {
        viewHolder.textViewCreationDate.setText(dateFormat.format(new Date(timestamp)));
      } else {
        viewHolder.textViewCreationDate.setText(null);
      }
    } else {
      viewHolder.textViewKeyOwner.setText(null);
      viewHolder.textViewKeywords.setText(null);
      viewHolder.textViewCreationDate.setText(null);
    }

    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (listener != null) {
          listener.onKeySelected(viewHolder.getAdapterPosition(), nodeKeyDetails);
        }
      }
    });
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  public void swap(List<NodeKeyDetails> nodeKeyDetailsList) {
    this.list = nodeKeyDetailsList;
    notifyDataSetChanged();
  }

  public interface OnKeySelectedListener {
    void onKeySelected(int position, NodeKeyDetails nodeKeyDetails);
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    final TextView textViewKeyOwner;
    final TextView textViewKeywords;
    final TextView textViewCreationDate;

    ViewHolder(View view) {
      super(view);
      textViewKeyOwner = view.findViewById(R.id.textViewKeyOwner);
      textViewKeywords = view.findViewById(R.id.textViewKeywords);
      textViewCreationDate = view.findViewById(R.id.textViewCreationDate);
    }
  }
}
