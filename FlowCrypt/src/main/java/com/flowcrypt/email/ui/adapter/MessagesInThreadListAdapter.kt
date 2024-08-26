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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.jakarta.mail.internet.personalOrEmail
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
class MessagesInThreadListAdapter : ListAdapter<MessageEntity,
    MessagesInThreadListAdapter.ViewHolder>(DIFF_UTIL_ITEM_CALLBACK) {

  private val collapsedStates = mutableMapOf<Long, Boolean>()
  private val fullList: MutableList<MessageEntity> = mutableListOf()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_message_in_thread, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (position == 0) {
      holder.itemView.context.toast("${collapsedStates[getItem(position).uid]}")
    }
    holder.bindTo(getItem(position), position == currentList.size - 1)
  }

  override fun submitList(list: List<MessageEntity>?) {
    fullList.clear()
    fullList.addAll(list ?: emptyList())
    super.submitList(list?.filterIndexed { index, _ -> index == list.size - 1 })
  }

  fun showAll() {
    super.submitList(fullList)
  }

  inner class ViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val binding = ItemMessageInThreadBinding.bind(itemView)

    fun bindTo(item: MessageEntity, isTheLast: Boolean) {
      val context = itemView.context
      val isCollapsed = collapsedStates[item.uid]
      binding.groupCollapsibleContent.visibleOrGone(collapsedStates[item.uid] ?: false)
      binding.header.setOnClickListener {
        val newState = !binding.groupCollapsibleContent.isVisible
        collapsedStates[item.uid] = newState
        binding.groupCollapsibleContent.visibleOrGone(newState)
      }

      val senderAddress = EmailUtil.getFirstAddressString(item.from)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )
      binding.textViewSenderAddress.text = senderAddress
      binding.tVTo.text = prepareToText(context, item)
      binding.textViewDate.text =
        DateTimeUtil.formatSameDayTime(context, item.receivedDate ?: 0)

      if (isTheLast && isCollapsed == null) {
        binding.header.callOnClick()
      }
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