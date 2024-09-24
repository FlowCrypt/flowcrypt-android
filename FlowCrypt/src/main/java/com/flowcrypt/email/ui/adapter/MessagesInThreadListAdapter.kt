/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadBinding
import com.flowcrypt.email.databinding.ItemThreadHeaderBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.color.MaterialColors

/**
 * @author Denys Bondarenko
 */
class MessagesInThreadListAdapter(private val onMessageClickListener: OnMessageClickListener) :
  ListAdapter<MessagesInThreadListAdapter.Item, MessagesInThreadListAdapter.BaseViewHolder>(
    DIFF_UTIL_ITEM_CALLBACK
  ) {

  override fun getItemViewType(position: Int): Int {
    return when (position) {
      0 -> Type.HEADER.id
      else -> Type.MESSAGE.id
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return when (viewType) {
      Type.HEADER.id -> HeaderViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_thread_header, parent, false)
      )

      Type.MESSAGE.id -> MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_message_in_thread, parent, false)
      )

      else -> error("Unreachable")
    }
  }

  override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
    when (holder.itemViewType) {
      Type.HEADER.id -> {
        val header = (getItem(position) as? Header) ?: return
        (holder as? HeaderViewHolder)?.bindTo(header)
      }

      Type.MESSAGE.id -> {
        val message = (getItem(position) as? Message) ?: return
        (holder as? MessageViewHolder)?.bindTo(message.messageEntity, onMessageClickListener)
      }

      else -> error("Unreachable")
    }
  }

  interface OnMessageClickListener {
    fun onMessageClick(messageEntity: MessageEntity)
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

  inner class HeaderViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemThreadHeaderBinding.bind(itemView)
    private val gmailApiLabelsListAdapter = GmailApiLabelsListAdapter(
      object : GmailApiLabelsListAdapter.OnLabelClickListener {
        override fun onLabelClick(label: GmailApiLabelsListAdapter.Label) {
          itemView.context.toast("fix me")
        }
      })

    fun bindTo(header: Header) {
      binding.textViewSubject.text = header.subject

      binding.recyclerViewLabels.apply {
        layoutManager = FlexboxLayoutManager(context).apply {
          flexDirection = FlexDirection.ROW
          justifyContent = JustifyContent.FLEX_START
        }
        addItemDecoration(
          MarginItemDecoration(
            marginRight = resources.getDimensionPixelSize(R.dimen.default_margin_small),
            marginTop = resources.getDimensionPixelSize(R.dimen.default_margin_small)
          )
        )
        adapter = gmailApiLabelsListAdapter
      }

      gmailApiLabelsListAdapter.submitList(header.labels)
    }
  }

  inner class MessageViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemMessageInThreadBinding.bind(itemView)

    fun bindTo(item: MessageEntity, onMessageClickListener: OnMessageClickListener) {
      val context = itemView.context
      itemView.setOnClickListener { onMessageClickListener.onMessageClick(item) }
      val senderAddress = item.generateFromText(context)
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

        if (item.isDraft) {
          val spannableStringBuilder = SpannableStringBuilder(text)
          spannableStringBuilder.append(" ")
          val timeSpannable = SpannableString("(${context.getString(R.string.draft)})")
          timeSpannable.setSpan(
            ForegroundColorSpan(Color.RED), 0, timeSpannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
          )
          spannableStringBuilder.append(timeSpannable)
          text = spannableStringBuilder
        }
      }
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

  abstract class Item(val type: Type) {
    abstract val id: Long
    abstract fun areContentsTheSame(other: Any?): Boolean
  }

  data class Message(val messageEntity: MessageEntity) : Item(Type.MESSAGE) {
    override val id: Long
      get() = messageEntity.uid

    override fun areContentsTheSame(other: Any?): Boolean {
      return this == other
    }
  }

  data class Header(
    val subject: String? = null,
    val labels: List<GmailApiLabelsListAdapter.Label>
  ) : Item(Type.HEADER) {
    override val id: Long
      get() = Long.MIN_VALUE

    override fun areContentsTheSame(other: Any?): Boolean {
      return this == other
    }
  }

  companion object {
    private val DIFF_UTIL_ITEM_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
      override fun areItemsTheSame(oldItem: Item, newItem: Item) =
        oldItem.id == newItem.id

      override fun areContentsTheSame(oldItem: Item, newItem: Item) =
        oldItem.areContentsTheSame(newItem)
    }

    enum class Type(val id: Int) {
      HEADER(0), MESSAGE(1)
    }
  }
}