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
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.GeneralUtil


/**
 * @author Denys Bondarenko
 */
class PrvKeysRecyclerViewAdapter(
  val pgpKeyDetailsList: MutableList<PgpKeyDetails> = mutableListOf()
) : ListAdapter<PgpKeyDetails, PrvKeysRecyclerViewAdapter.ViewHolder>
  (DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.prv_key_item, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tVEmail: TextView = itemView.findViewById(R.id.tVEmail)
    private val tVFingerprint: TextView = itemView.findViewById(R.id.tVFingerprint)

    fun bind(pgpKeyDetails: PgpKeyDetails) {
      tVEmail.text = pgpKeyDetails.getUserIdsAsSingleString()
      tVFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = pgpKeyDetails.fingerprint, groupSize = 4
      )
    }
  }


  class DiffUtilCallBack : DiffUtil.ItemCallback<PgpKeyDetails>() {
    override fun areItemsTheSame(oldItem: PgpKeyDetails, newItem: PgpKeyDetails) =
      oldItem.fingerprint == newItem.fingerprint

    override fun areContentsTheSame(oldItem: PgpKeyDetails, newItem: PgpKeyDetails) =
      oldItem == newItem
  }
}
