/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
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
class ImportPgpContactsRecyclerViewAdapter
  : RecyclerView.Adapter<ImportPgpContactsRecyclerViewAdapter.ViewHolder>() {

  val publicKeys = mutableListOf<PublicKeyInfo>()
  var contactActionsListener: ContactActionsListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.import_pgp_contact_item, parent, false)
    )
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val context = viewHolder.itemView.context
    val publicKeyInfo = publicKeys[position]

    viewHolder.buttonUpdateContact.visibility = View.GONE
    viewHolder.buttonSaveContact.visibility = View.GONE

    if (!TextUtils.isEmpty(publicKeyInfo.keyOwner)) {
      viewHolder.textViewKeyOwnerTemplate.text = context.getString(
        R.string.template_message_part_public_key_owner,
        publicKeyInfo.keyOwner
      )
    }

    UIUtil.setHtmlTextToTextView(
      context.getString(
        R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", publicKeyInfo.fingerprint, 4)
      ), viewHolder.textViewFingerprintTemplate
    )

    if (publicKeyInfo.hasPgp()) {
      viewHolder.textViewAlreadyImported.visibility = View.VISIBLE
    } else {
      viewHolder.textViewAlreadyImported.visibility = View.GONE
      viewHolder.buttonSaveContact.visibility = View.VISIBLE
      viewHolder.buttonSaveContact.setOnClickListener { v ->
        saveContact(
          viewHolder.adapterPosition,
          v,
          context,
          publicKeyInfo
        )
      }
    }
  }

  override fun getItemCount(): Int {
    return publicKeys.size
  }

  fun swap(newList: Collection<PublicKeyInfo>) {
    publicKeys.clear()
    publicKeys.addAll(newList)
    notifyDataSetChanged()
  }

  private fun saveContact(position: Int, v: View, context: Context, publicKeyInfo: PublicKeyInfo) {
    val pgpContact = PgpContact(
      publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey, true,
      null, publicKeyInfo.fingerprint, 0
    )

    contactActionsListener?.onSaveContactClick(publicKeyInfo)
    Toast.makeText(context, R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show()
    v.visibility = View.GONE
    publicKeyInfo.recipientWithPubKeys =
      RecipientWithPubKeys(pgpContact.toRecipientEntity(), listOf(pgpContact.toPubKey()))
    notifyItemChanged(position)
  }

  private fun updateContact(
    position: Int,
    v: View,
    context: Context,
    publicKeyInfo: PublicKeyInfo
  ) {
    val pgpContact = PgpContact(
      publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey, true,
      null, publicKeyInfo.fingerprint, 0
    )

    contactActionsListener?.onUpdateContactClick(publicKeyInfo)
    Toast.makeText(context, R.string.contact_successfully_updated, Toast.LENGTH_SHORT).show()
    v.visibility = View.GONE
    publicKeyInfo.recipientWithPubKeys =
      RecipientWithPubKeys(pgpContact.toRecipientEntity(), listOf(pgpContact.toPubKey()))
    notifyItemChanged(position)
  }

  /**
   * The view holder implementation for a better performance.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var textViewKeyOwnerTemplate: TextView = itemView.findViewById(R.id.textViewKeyOwnerTemplate)
    var textViewFingerprintTemplate: TextView =
      itemView.findViewById(R.id.textViewFingerprintTemplate)
    var textViewAlreadyImported: TextView = itemView.findViewById(R.id.textViewAlreadyImported)
    var buttonSaveContact: Button = itemView.findViewById(R.id.buttonSaveContact)
    var buttonUpdateContact: Button = itemView.findViewById(R.id.buttonUpdateContact)
  }

  interface ContactActionsListener {
    fun onSaveContactClick(publicKeyInfo: PublicKeyInfo)
    fun onUpdateContactClick(publicKeyInfo: PublicKeyInfo)
  }
}
