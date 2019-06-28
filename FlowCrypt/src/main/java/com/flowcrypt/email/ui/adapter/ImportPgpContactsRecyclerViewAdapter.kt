/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.PublicKeyInfo
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This adapter can be used for showing information about [PgpContact]s when we want to import them
 *
 * @author Denis Bondarenko
 * Date: 09.05.2018
 * Time: 08:07
 * E-mail: DenBond7@gmail.com
 */
class ImportPgpContactsRecyclerViewAdapter(private val publicKeys: List<PublicKeyInfo>)
  : RecyclerView.Adapter<ImportPgpContactsRecyclerViewAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.import_pgp_contact_item, parent, false))
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val context = viewHolder.itemView.context
    val publicKeyInfo = publicKeys[position]

    viewHolder.buttonUpdateContact.visibility = View.GONE
    viewHolder.buttonSaveContact.visibility = View.GONE

    if (!TextUtils.isEmpty(publicKeyInfo.keyOwner)) {
      viewHolder.textViewKeyOwnerTemplate.text = context.getString(R.string.template_message_part_public_key_owner,
          publicKeyInfo.keyOwner)
    }

    UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_key_words,
        publicKeyInfo.keyWords), viewHolder.textViewKeyWordsTemplate)
    UIUtil.setHtmlTextToTextView(context.getString(R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", publicKeyInfo.fingerprint, 4)), viewHolder.textViewFingerprintTemplate)

    if (publicKeyInfo.hasPgpContact()) {
      if (publicKeyInfo.isUpdateEnabled) {
        viewHolder.textViewAlreadyImported.visibility = View.GONE
        viewHolder.buttonUpdateContact.visibility = View.VISIBLE
        viewHolder.buttonUpdateContact.setOnClickListener { v -> updateContact(viewHolder.adapterPosition, v, context, publicKeyInfo) }
      } else {
        viewHolder.textViewAlreadyImported.visibility = View.VISIBLE
      }
    } else {
      viewHolder.textViewAlreadyImported.visibility = View.GONE
      viewHolder.buttonSaveContact.visibility = View.VISIBLE
      viewHolder.buttonSaveContact.setOnClickListener { v -> saveContact(viewHolder.adapterPosition, v, context, publicKeyInfo) }
    }
  }

  override fun getItemCount(): Int {
    return publicKeys.size
  }

  private fun saveContact(position: Int, v: View, context: Context, publicKeyInfo: PublicKeyInfo) {
    val pgpContact = PgpContact(publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey, true,
        null, publicKeyInfo.fingerprint, publicKeyInfo.longId, publicKeyInfo.keyWords, 0)

    val uri = ContactsDaoSource().addRow(context, pgpContact)
    if (uri != null) {
      notifyItemChanged(position)
      Toast.makeText(context, R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show()
      v.visibility = View.GONE
      publicKeyInfo.pgpContact = pgpContact
    } else {
      Toast.makeText(context, R.string.error_occurred_while_saving_contact, Toast.LENGTH_SHORT).show()
    }
  }

  private fun updateContact(position: Int, v: View, context: Context, publicKeyInfo: PublicKeyInfo) {
    val pgpContact = PgpContact(publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey, true,
        null, publicKeyInfo.fingerprint, publicKeyInfo.longId, publicKeyInfo.keyWords, 0)

    val isUpdated = ContactsDaoSource().updatePgpContact(context, pgpContact) > 0
    if (isUpdated) {
      Toast.makeText(context, R.string.contact_successfully_updated, Toast.LENGTH_SHORT).show()
      v.visibility = View.GONE
      publicKeyInfo.pgpContact = pgpContact
      notifyItemChanged(position)
    } else {
      Toast.makeText(context, R.string.error_occurred_while_updating_contact, Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * The view holder implementation for a better performance.
   */
  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var textViewKeyOwnerTemplate: TextView = itemView.findViewById(R.id.textViewKeyOwnerTemplate)
    var textViewKeyWordsTemplate: TextView = itemView.findViewById(R.id.textViewKeyWordsTemplate)
    var textViewFingerprintTemplate: TextView = itemView.findViewById(R.id.textViewFingerprintTemplate)
    var textViewAlreadyImported: TextView = itemView.findViewById(R.id.textViewAlreadyImported)
    var buttonSaveContact: Button = itemView.findViewById(R.id.buttonSaveContact)
    var buttonUpdateContact: Button = itemView.findViewById(R.id.buttonUpdateContact)
  }
}
