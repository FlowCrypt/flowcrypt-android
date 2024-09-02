/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import com.google.android.material.color.MaterialColors

/**
 * @author Denys Bondarenko
 */
class MessagesInThreadListAdapter(private val onMessageClickListener: OnMessageClickListener) :
  ListAdapter<MessageEntity, MessagesInThreadListAdapter.ViewHolder>(DIFF_UTIL_ITEM_CALLBACK) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_message_in_thread, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position), onMessageClickListener)
  }

  interface OnMessageClickListener {
    fun onMessageClick(messageEntity: MessageEntity)
  }

  inner class ViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val binding = ItemMessageInThreadBinding.bind(itemView)

    fun bindTo(item: MessageEntity, onMessageClickListener: OnMessageClickListener) {
      val context = itemView.context
      itemView.setOnClickListener { onMessageClickListener.onMessageClick(item) }
      val senderAddress = EmailUtil.getFirstAddressString(item.from)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )
      binding.textViewSnippet.text = if (item.hasPgp == true) {
        context.getString(R.string.preview_is_not_available_for_messages_with_pgp)
      } else {
        item.snippet
      }
      binding.tVTo.text = item.generateToText(context)
      binding.textViewSender.apply {
        text = senderAddress
        if (item.isSeen) {
          setTypeface(null, Typeface.NORMAL)
          setTextColor(
            MaterialColors.getColor(
              context,
              com.google.android.material.R.attr.colorOnSurfaceVariant,
              Color.BLACK
            )
          )
        } else {
          setTypeface(null, Typeface.BOLD)
          setTextColor(
            MaterialColors.getColor(context, R.attr.itemTitleColor, Color.BLACK)
          )
        }
      }
      binding.textViewSender.text = senderAddress
      binding.textViewDate.apply {
        text = DateTimeUtil.formatSameDayTime(context, item.receivedDate ?: 0)
        if (item.isSeen) {
          setTypeface(null, Typeface.NORMAL)
          setTextColor(
            MaterialColors.getColor(context, R.attr.itemSubTitleColor, Color.BLACK)
          )
        } else {
          setTypeface(null, Typeface.BOLD)
          setTextColor(
            MaterialColors.getColor(context, R.attr.itemTitleColor, Color.BLACK)
          )
        }
      }
      binding.viewHasPgp.visibleOrGone(item.hasPgp == true || item.isEncrypted == true)
      binding.viewHasAttachments.visibleOrGone(item.hasAttachments == true)
      binding.textViewDate.setTypeface(null, if (item.isSeen) Typeface.NORMAL else Typeface.BOLD)
    }
  }

  companion object {
    private val DIFF_UTIL_ITEM_CALLBACK = object : DiffUtil.ItemCallback<MessageEntity>() {
      override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
        oldItem.uid == newItem.uid

      override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
        oldItem == newItem
    }
  }
}