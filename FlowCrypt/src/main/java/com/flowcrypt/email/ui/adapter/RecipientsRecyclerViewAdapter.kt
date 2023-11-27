/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.databinding.ContactItemBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone

/**
 * This adapter describes logic to prepare show recipients from the database.
 *
 * @author Denys Bondarenko
 */
class RecipientsRecyclerViewAdapter(
  val isDeleteEnabled: Boolean = true,
  private val onRecipientActionsListener: OnRecipientActionsListener? = null
) : ListAdapter<RecipientEntity.WithPgpMarker, RecipientsRecyclerViewAdapter.ViewHolder>(
  DIFF_CALLBACK
) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it, onRecipientActionsListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ContactItemBinding.bind(itemView)

    fun bind(
      recipientEntity: RecipientEntity.WithPgpMarker,
      onRecipientActionsListener: OnRecipientActionsListener?
    ) {
      if (recipientEntity.name.isNullOrEmpty()) {
        binding.tVName.gone()
        binding.tVEmail.gone()
        binding.tVOnlyEmail.visible()
        binding.tVOnlyEmail.text = recipientEntity.email
        binding.tVEmail.text = null
        binding.tVName.text = null
      } else {
        binding.tVName.visible()
        binding.tVEmail.visible()
        binding.tVOnlyEmail.gone()
        binding.tVEmail.text = recipientEntity.email
        binding.tVName.text = recipientEntity.name
        binding.tVOnlyEmail.text = null
      }

      if (isDeleteEnabled) {
        binding.iBtDeleteContact.visible()
        binding.iBtDeleteContact.setOnClickListener {
          onRecipientActionsListener?.onDeleteRecipient(recipientEntity)
        }
      } else {
        binding.iBtDeleteContact.invisible()
      }

      binding.imageViewPgp.visibleOrGone(recipientEntity.hasPgp)

      itemView.setOnClickListener {
        onRecipientActionsListener?.onRecipientClick(recipientEntity)
      }
    }
  }

  interface OnRecipientActionsListener {
    fun onDeleteRecipient(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker)
    fun onRecipientClick(recipientEntityWithPgpMarker: RecipientEntity.WithPgpMarker)
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipientEntity.WithPgpMarker>() {
      override fun areItemsTheSame(
        oldItem: RecipientEntity.WithPgpMarker,
        newItem: RecipientEntity.WithPgpMarker
      ) = oldItem.email == newItem.email

      override fun areContentsTheSame(
        oldItem: RecipientEntity.WithPgpMarker,
        newItem: RecipientEntity.WithPgpMarker
      ) = oldItem == newItem
    }
  }
}
