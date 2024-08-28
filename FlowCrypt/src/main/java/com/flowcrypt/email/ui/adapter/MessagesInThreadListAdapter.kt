/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.jakarta.mail.internet.personalOrEmail
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import jakarta.mail.internet.InternetAddress

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
      binding.textViewSnippet.text = item.snippet
      binding.textViewSender.text = senderAddress
      binding.tVTo.text = prepareToText(context, item)
      binding.textViewDate.text = DateTimeUtil.formatSameDayTime(context, item.receivedDate ?: 0)
      binding.viewHasPgp.visibleOrGone(item.hasPgp == true || item.isEncrypted == true)
      binding.viewHasAttachments.visibleOrGone(item.hasAttachments == true)
    }

    private fun prepareToText(context: Context, messageEntity: MessageEntity): String {
      val stringBuilder = SpannableStringBuilder()
      val meAddress = messageEntity.to.firstOrNull {
        it.address.equals(messageEntity.account, true)
      }
      val leftAddresses: List<InternetAddress>
      if (meAddress == null) {
        leftAddresses = messageEntity.to
      } else {
        stringBuilder.append(context.getString(R.string.me))
        leftAddresses = ArrayList(messageEntity.to) - meAddress
        if (leftAddresses.isNotEmpty()) {
          stringBuilder.append(", ")
        }
      }

      val to = leftAddresses.foldIndexed(stringBuilder) { index, builder, it ->
        builder.append(it.personalOrEmail)
        if (index != leftAddresses.size - 1) {
          builder.append(",")
        }
        builder
      }.toSpannable()

      return context.getString(R.string.to_receiver, to)
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