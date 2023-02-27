/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visible

/**
 * This adapter describes logic to prepare show recipients from the database.
 *
 * @author Denys Bondarenko
 */
class RecipientsRecyclerViewAdapter(
  val isDeleteEnabled: Boolean = true,
  private val onRecipientActionsListener: OnRecipientActionsListener? = null
) :
  ListAdapter<RecipientEntity, RecipientsRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it, onRecipientActionsListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tVName: TextView = itemView.findViewById(R.id.tVName)
    private val tVEmail: TextView = itemView.findViewById(R.id.tVEmail)
    private val tVOnlyEmail: TextView = itemView.findViewById(R.id.tVOnlyEmail)
    private val iBtDeleteContact: ImageButton = itemView.findViewById(R.id.iBtDeleteContact)

    fun bind(
      recipientEntity: RecipientEntity,
      onRecipientActionsListener: OnRecipientActionsListener?
    ) {
      if (recipientEntity.name.isNullOrEmpty()) {
        tVName.gone()
        tVEmail.gone()
        tVOnlyEmail.visible()
        tVOnlyEmail.text = recipientEntity.email
        tVEmail.text = null
        tVName.text = null
      } else {
        tVName.visible()
        tVEmail.visible()
        tVOnlyEmail.gone()
        tVEmail.text = recipientEntity.email
        tVName.text = recipientEntity.name
        tVOnlyEmail.text = null
      }

      if (isDeleteEnabled) {
        iBtDeleteContact.visible()
        iBtDeleteContact.setOnClickListener {
          onRecipientActionsListener?.onDeleteRecipient(recipientEntity)
        }
      } else {
        iBtDeleteContact.gone()
      }

      itemView.setOnClickListener {
        onRecipientActionsListener?.onRecipientClick(recipientEntity)
      }
    }
  }

  interface OnRecipientActionsListener {
    fun onDeleteRecipient(recipientEntity: RecipientEntity)
    fun onRecipientClick(recipientEntity: RecipientEntity)
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<RecipientEntity>() {
    override fun areItemsTheSame(oldItem: RecipientEntity, newItem: RecipientEntity) =
      oldItem.email == newItem.email

    override fun areContentsTheSame(oldItem: RecipientEntity, newItem: RecipientEntity) =
      oldItem == newItem
  }
}
