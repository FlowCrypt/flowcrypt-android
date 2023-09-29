/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.databinding.ItemGmailLabelWithCheckboxBinding
import com.flowcrypt.email.extensions.kotlin.parseAsColorBasedOnDefaultSettings
import com.flowcrypt.email.model.LabelWithChoice

/**
 * @author Denys Bondarenko
 */
class GmailApiLabelsWithChoiceListAdapter :
  ListAdapter<LabelWithChoice, GmailApiLabelsWithChoiceListAdapter.ViewHolder>(
    DiffUtilCallBack()
  ) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.item_gmail_label_with_checkbox, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  inner class ViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val binding = ItemGmailLabelWithCheckboxBinding.bind(itemView)

    fun bindTo(item: LabelWithChoice) {
      itemView.setOnClickListener { binding.checkBox.toggle() }
      binding.textViewLabel.text = item.name
      binding.checkBox.isChecked = item.isChecked

      val folderIconResourceId = R.drawable.ic_label_24dp
      when {
        item.name == GmailApiHelper.LABEL_INBOX -> {
          binding.imageView.setImageResource(R.drawable.ic_mail_24dp)
        }

        item.backgroundColor != null -> {
          val drawable = ContextCompat.getDrawable(itemView.context, folderIconResourceId)
          val color = item.backgroundColor.parseAsColorBasedOnDefaultSettings(itemView.context)
          drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
          )
          binding.imageView.setImageDrawable(drawable)
        }

        else -> {
          binding.imageView.setImageResource(folderIconResourceId)
        }
      }
    }
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<LabelWithChoice>() {
    override fun areItemsTheSame(oldItem: LabelWithChoice, newItem: LabelWithChoice): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LabelWithChoice, newItem: LabelWithChoice): Boolean {
      return oldItem == newItem
    }
  }
}
