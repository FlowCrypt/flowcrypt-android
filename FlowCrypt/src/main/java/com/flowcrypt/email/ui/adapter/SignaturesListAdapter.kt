/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.databinding.SignatureItemBinding

/**
 * @author Denys Bondarenko
 */
class SignaturesListAdapter :
  ListAdapter<AccountAliasesEntity, SignaturesListAdapter.ViewHolder>(DIFF_CALLBACK) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.signature_item, parent, false)
    )
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    viewHolder.bind(getItem(position))
  }
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = SignatureItemBinding.bind(itemView)
    fun bind(accountAliasesEntity: AccountAliasesEntity) {
      binding.textViewSendAs.text = accountAliasesEntity.sendAsEmail
      binding.textViewSignature.text = accountAliasesEntity.signature
    }
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AccountAliasesEntity>() {
      override fun areItemsTheSame(
        oldItem: AccountAliasesEntity,
        newItem: AccountAliasesEntity
      ): Boolean = oldItem.id == newItem.id

      override fun areContentsTheSame(
        oldItem: AccountAliasesEntity,
        newItem: AccountAliasesEntity
      ): Boolean = oldItem == newItem
    }
  }
}