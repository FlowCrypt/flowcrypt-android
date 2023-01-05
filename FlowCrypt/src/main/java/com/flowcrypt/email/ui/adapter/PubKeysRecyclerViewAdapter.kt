/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import java.util.Date

/**
 * @author Denis Bondarenko
 *         Date: 11/19/21
 *         Time: 1:50 PM
 *         E-mail: DenBond7@gmail.com
 */
class PubKeysRecyclerViewAdapter(private val onPubKeyActionsListener: OnPubKeyActionsListener) :
  ListAdapter<PublicKeyEntity, PubKeysRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {

  private var dateFormat: java.text.DateFormat? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.public_key_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (dateFormat == null) {
      dateFormat = DateTimeUtil.getPgpDateFormat(holder.itemView.context)
    }

    getItem(position)?.let { holder.bind(it, onPubKeyActionsListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tVPrimaryUser: TextView = itemView.findViewById(R.id.tVPrimaryUser)
    private val tVFingerprint: TextView = itemView.findViewById(R.id.tVFingerprint)
    private val tVCreationDate: TextView = itemView.findViewById(R.id.tVCreationDate)

    fun bind(
      publicKeyEntity: PublicKeyEntity,
      onPubKeyActionsListener: OnPubKeyActionsListener?
    ) {
      tVPrimaryUser.text = publicKeyEntity.pgpKeyDetails?.getUserIdsAsSingleString()
      tVFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = publicKeyEntity.fingerprint, groupSize = 4
      )

      val timestamp = publicKeyEntity.pgpKeyDetails?.created ?: 0
      if (timestamp != -1L) {
        tVCreationDate.text = dateFormat?.format(Date(timestamp))
      } else {
        tVCreationDate.text = null
      }

      itemView.setOnClickListener {
        onPubKeyActionsListener?.onPubKeyClick(publicKeyEntity)
      }
    }
  }

  interface OnPubKeyActionsListener {
    fun onPubKeyClick(publicKeyEntity: PublicKeyEntity)
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<PublicKeyEntity>() {
    override fun areItemsTheSame(oldItem: PublicKeyEntity, newItem: PublicKeyEntity) =
      oldItem.recipient == newItem.recipient && oldItem.fingerprint == newItem.fingerprint

    override fun areContentsTheSame(oldItem: PublicKeyEntity, newItem: PublicKeyEntity) =
      oldItem == newItem
  }
}
