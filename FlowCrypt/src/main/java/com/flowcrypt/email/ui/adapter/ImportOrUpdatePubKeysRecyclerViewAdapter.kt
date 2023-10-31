/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This adapter can be used for showing information about public keys when we want to import them
 *
 * @author Denys Bondarenko
 */
class ImportOrUpdatePubKeysRecyclerViewAdapter(
  private val pubKeyActionsListener: PubKeyActionsListener? = null
) : ListAdapter<PgpKeyRingDetails, ImportOrUpdatePubKeysRecyclerViewAdapter.ViewHolder>
  (DiffUtilCallBack()) {

  val existingPubKeyEntities = mutableMapOf<String, PublicKeyEntity>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.import_pgp_contact_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it, pubKeyActionsListener) }
  }

  fun swap(map: Map<String, PublicKeyEntity>) {
    existingPubKeyEntities.clear()
    existingPubKeyEntities.putAll(map)
    notifyItemRangeChanged(0, itemCount)
  }

  /**
   * The view holder implementation for a better performance.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewKeyOwnerTemplate: TextView =
      itemView.findViewById(R.id.textViewKeyOwnerTemplate)
    private val textViewFingerprintTemplate: TextView =
      itemView.findViewById(R.id.textViewFingerprintTemplate)
    private val textViewStatus: TextView =
      itemView.findViewById(R.id.textViewStatus)
    private val buttonSaveContact: Button = itemView.findViewById(R.id.buttonSaveContact)
    private val buttonUpdateContact: Button = itemView.findViewById(R.id.buttonUpdateContact)

    fun bind(
      pgpKeyRingDetails: PgpKeyRingDetails,
      pubKeyActionsListener: PubKeyActionsListener?
    ) {
      val context = itemView.context
      val address = pgpKeyRingDetails.getPrimaryInternetAddress()?.address?.lowercase() ?: ""
      val existingPubKeyEntity = existingPubKeyEntities[address + pgpKeyRingDetails.fingerprint]

      buttonUpdateContact.visibility = View.GONE
      buttonSaveContact.visibility = View.GONE

      if (pgpKeyRingDetails.getPrimaryInternetAddress()?.address?.isNotEmpty() == true) {
        textViewKeyOwnerTemplate.text = context.getString(
          R.string.template_message_part_public_key_owner,
          address
        )
      }

      UIUtil.setHtmlTextToTextView(
        context.getString(
          R.string.template_message_part_public_key_fingerprint,
          GeneralUtil.doSectionsInText(" ", pgpKeyRingDetails.fingerprint, 4)
        ), textViewFingerprintTemplate
      )

      if (!pgpKeyRingDetails.usableForEncryption) {
        textViewStatus.visible()
        textViewStatus.text = context.getString(R.string.cannot_be_used_for_encryption)
        textViewStatus.setTextColor(UIUtil.getColor(context, R.color.red))
        return
      } else {
        textViewStatus.setTextColor(UIUtil.getColor(context, R.color.colorPrimary))
      }

      if (existingPubKeyEntity != null) {
        if (pgpKeyRingDetails.isNewerThan(existingPubKeyEntity.pgpKeyRingDetails)) {
          buttonUpdateContact.visible()
          buttonUpdateContact.setOnClickListener {
            if (GeneralUtil.isEmailValid(address)) {
              pubKeyActionsListener?.onUpdatePubKeyClick(
                pgpKeyRingDetails = pgpKeyRingDetails,
                existingPublicKeyEntity = existingPubKeyEntity
              )
            }
          }
        }
        textViewStatus.text = context.getString(R.string.already_imported)
        textViewStatus.visible()
      } else {
        textViewStatus.gone()
        buttonSaveContact.visible()
        buttonSaveContact.setOnClickListener {
          if (GeneralUtil.isEmailValid(address)) {
            pubKeyActionsListener?.onSavePubKeyClick(
              pgpKeyRingDetails = pgpKeyRingDetails
            )
          }
        }
      }
    }
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<PgpKeyRingDetails>() {
    override fun areItemsTheSame(oldItem: PgpKeyRingDetails, newItem: PgpKeyRingDetails) =
      oldItem.fingerprint == newItem.fingerprint

    override fun areContentsTheSame(oldItem: PgpKeyRingDetails, newItem: PgpKeyRingDetails) =
      oldItem == newItem
  }

  interface PubKeyActionsListener {
    fun onSavePubKeyClick(
      pgpKeyRingDetails: PgpKeyRingDetails
    )

    fun onUpdatePubKeyClick(
      pgpKeyRingDetails: PgpKeyRingDetails,
      existingPublicKeyEntity: PublicKeyEntity
    )
  }
}
