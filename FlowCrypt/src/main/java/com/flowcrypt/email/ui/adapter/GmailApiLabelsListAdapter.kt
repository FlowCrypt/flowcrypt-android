/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.ItemLabelBadgeBinding
import com.flowcrypt.email.extensions.kotlin.parseAsColorBasedOnDefaultSettings

/**
 * @author Denys Bondarenko
 */
class GmailApiLabelsListAdapter : ListAdapter<GmailApiLabelsListAdapter.Label,
    GmailApiLabelsListAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_label_badge, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemLabelBadgeBinding.bind(itemView)

    fun bindTo(item: Label) {
      binding.textViewLabel.text = item.name
      binding.textViewLabel.setTextColor(
        item.textColor.parseAsColorBasedOnDefaultSettings(
          context = itemView.context,
          defaultColorResourceId = R.color.white,
          secondDefaultColorResourceId = R.color.white
        )
      )
      itemView.backgroundTintList = ColorStateList.valueOf(
        item.backgroundColor.parseAsColorBasedOnDefaultSettings(context = itemView.context)
      )
    }
  }

  data class Label(
    val name: String,
    val backgroundColor: String? = null,
    val textColor: String? = null
  )

  class DiffUtilCallBack : DiffUtil.ItemCallback<Label>() {
    override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean {
      return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean {
      return oldItem == newItem
    }
  }
}
