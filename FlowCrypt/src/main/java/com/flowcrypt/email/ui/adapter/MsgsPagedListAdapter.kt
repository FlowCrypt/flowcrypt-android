/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.MessagesListItemBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.UIUtil
import java.util.regex.Pattern
import javax.mail.internet.InternetAddress

/**
 * This class is responsible for displaying the message in the list.
 *
 * @author Denis Bondarenko
 *         Date: 12/15/19
 *         Time: 4:48 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgsPagedListAdapter(
  var currentFolder: LocalFolder? = null,
  private val onMessageClickListener: OnMessageClickListener? = null
) : PagingDataAdapter<MessageEntity, MsgsPagedListAdapter.MessageViewHolder>(ITEM_CALLBACK) {
  private val senderNamePattern: Pattern = prepareSenderNamePattern()

  override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
    val msgEntity = getItem(position) ?: return
    holder.bind(msgEntity)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
    return MessageViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.messages_list_item, parent, false)
    )
  }

  /**
   * Prepare a [Pattern] which will be used for finding some information in the sender name.
   * This pattern is case insensitive.
   *
   * @return A generated [Pattern].
   */
  private fun prepareSenderNamePattern(): Pattern {
    val domains = ArrayList<String>()
    domains.add(JavaEmailConstants.EMAIL_PROVIDER_GMAIL)
    domains.add(JavaEmailConstants.EMAIL_PROVIDER_YAHOO)
    domains.add(JavaEmailConstants.EMAIL_PROVIDER_LIVE)
    domains.add(JavaEmailConstants.EMAIL_PROVIDER_OUTLOOK)

    val stringBuilder = StringBuilder()
    stringBuilder.append("@")
    stringBuilder.append("(")
    stringBuilder.append(domains[0])

    for (i in 1 until domains.size) {
      stringBuilder.append("|")
      stringBuilder.append(domains[i])
    }
    stringBuilder.append(")$")

    return Pattern.compile(stringBuilder.toString(), Pattern.CASE_INSENSITIVE)
  }

  /**
   * Prepare the sender name.
   *
   *  * Remove common mail domains: gmail.com, yahoo.com, live.com, outlook.com
   *
   *
   * @param name An incoming name
   * @return A generated sender name.
   */
  private fun prepareSenderName(name: String): String {
    return senderNamePattern.matcher(name).replaceFirst("")
  }

  private fun generateAddresses(internetAddresses: List<InternetAddress>?): String {
    if (internetAddresses == null) {
      return "null"
    }

    val iMax = internetAddresses.size - 1
    if (iMax == -1) {
      return ""
    }

    val b = StringBuilder()
    var i = 0
    while (true) {
      val address = internetAddresses[i]
      val displayName =
        if (TextUtils.isEmpty(address.personal)) address.address else address.personal
      b.append(displayName)
      if (i == iMax) {
        return prepareSenderName(b.toString())
      }
      b.append(", ")
      i++
    }
  }

  private fun generateOutboxStatus(context: Context?, messageState: MessageState): CharSequence {
    context ?: return ""
    val me = context.getString(R.string.me)
    var state = ""
    var stateTextColor = ContextCompat.getColor(context, R.color.red)

    when (messageState) {
      MessageState.NEW, MessageState.NEW_FORWARDED, MessageState.NEW_PASSWORD_PROTECTED -> {
        state = context.getString(R.string.preparing)
        stateTextColor = ContextCompat.getColor(context, R.color.colorAccent)
      }

      MessageState.QUEUED, MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER -> {
        state = context.getString(R.string.queued)
        stateTextColor = ContextCompat.getColor(context, R.color.colorAccent)
      }

      MessageState.SENDING -> {
        state = context.getString(R.string.sending)
        stateTextColor = ContextCompat.getColor(context, R.color.colorPrimary)
      }

      MessageState.ERROR_CACHE_PROBLEM,
      MessageState.ERROR_DURING_CREATION,
      MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
      MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
      MessageState.ERROR_SENDING_FAILED,
      MessageState.ERROR_PRIVATE_KEY_NOT_FOUND,
      MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER,
      MessageState.AUTH_FAILURE,
      MessageState.ERROR_PASSWORD_PROTECTED -> {
        stateTextColor = ContextCompat.getColor(context, R.color.red)

        when (messageState) {
          MessageState.ERROR_CACHE_PROBLEM -> state = context.getString(R.string.cache_error)

          MessageState.ERROR_DURING_CREATION -> state = context.getString(R.string.could_not_create)

          MessageState.ERROR_ORIGINAL_MESSAGE_MISSING -> state =
            context.getString(R.string.original_message_missing)

          MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND ->
            state = context.getString(R.string.original_attachment_not_found)

          MessageState.ERROR_SENDING_FAILED -> state =
            context.getString(R.string.cannot_send_message_unknown_error)

          MessageState.ERROR_PRIVATE_KEY_NOT_FOUND ->
            state = context.getString(R.string.could_not_create_no_key_available)

          MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER ->
            state = context.getString(R.string.cannot_save_copy_in_sent_folder)

          MessageState.AUTH_FAILURE ->
            state = context.getString(R.string.can_not_send_due_to_auth_failure)

          MessageState.ERROR_PASSWORD_PROTECTED ->
            state = context.getString(R.string.can_not_send_password_protected)

          else -> {
          }
        }
      }

      else -> {
      }
    }

    val meTextSize = context.resources.getDimensionPixelSize(R.dimen.default_text_size_big)
    val statusTextSize =
      context.resources.getDimensionPixelSize(R.dimen.default_text_size_very_small)

    val spannableStringMe = SpannableString(me)
    spannableStringMe.setSpan(
      AbsoluteSizeSpan(meTextSize),
      0,
      me.length,
      Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )

    val status = SpannableString(state)
    status.setSpan(
      AbsoluteSizeSpan(statusTextSize),
      0,
      state.length,
      Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )
    status.setSpan(
      ForegroundColorSpan(stateTextColor),
      0,
      state.length,
      Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )

    return TextUtils.concat(spannableStringMe, " ", status)
  }

  inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding: MessagesListItemBinding = MessagesListItemBinding.bind(itemView)

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
      object : ItemDetailsLookup.ItemDetails<Long>() {
        override fun getPosition(): Int = bindingAdapterPosition
        override fun getSelectionKey(): Long = itemId
      }

    fun bind(value: MessageEntity) {
      val context = itemView.context

      itemView.setOnClickListener {
        onMessageClickListener?.onMsgClick(value)
      }

      val subject = if (TextUtils.isEmpty(value.subject)) {
        context.getString(R.string.no_subject)
      } else {
        value.subject
      }

      val folderType = FoldersManager.getFolderType(currentFolder)
      if (folderType != null) {
        when (folderType) {
          FoldersManager.FolderType.SENT -> {
            binding.textViewSenderAddress.text = generateAddresses(value.to)
          }

          FoldersManager.FolderType.OUTBOX -> {
            val status = generateOutboxStatus(
              binding.textViewSenderAddress.context,
              value.msgState
            )
            binding.textViewSenderAddress.text = status
          }

          else -> {
            binding.textViewSenderAddress.text = generateAddresses(value.from)
          }
        }
      } else {
        binding.textViewSenderAddress.text = generateAddresses(value.from)
      }

      binding.textViewSubject.text = subject
      if (folderType === FoldersManager.FolderType.OUTBOX) {
        binding.textViewDate.text = DateTimeUtil.formatSameDayTime(context, value.sentDate)
      } else {
        binding.textViewDate.text = DateTimeUtil.formatSameDayTime(context, value.receivedDate)
      }

      if (value.isSeen) {
        changeViewsTypeface(Typeface.NORMAL)
        binding.textViewSenderAddress.setTextColor(UIUtil.getColor(context, R.color.dark))
        binding.textViewDate.setTextColor(UIUtil.getColor(context, R.color.gray))
      } else {
        changeViewsTypeface(Typeface.BOLD)
        binding.textViewSenderAddress.setTextColor(UIUtil.getColor(context, android.R.color.black))
        binding.textViewDate.setTextColor(UIUtil.getColor(context, android.R.color.black))
      }

      binding.imageViewAtts.visibleOrGone(value.hasAttachments == true)
      binding.viewIsEncrypted.visibleOrGone(value.isEncrypted == true)

      when (value.msgState) {
        MessageState.PENDING_ARCHIVING -> {
          binding.imageViewStatus.visible()
          binding.imageViewStatus.setBackgroundResource(R.drawable.ic_archive_blue_16dp)
        }

        MessageState.PENDING_MARK_UNREAD -> {
          binding.imageViewStatus.visible()
          binding.imageViewStatus.setBackgroundResource(R.drawable.ic_markunread_blue_16dp)
        }

        MessageState.PENDING_DELETING,
        MessageState.PENDING_DELETING_PERMANENTLY,
        MessageState.PENDING_EMPTY_TRASH -> {
          binding.imageViewStatus.visible()
          binding.imageViewStatus.setBackgroundResource(R.drawable.ic_delete_blue_16dp)
        }

        MessageState.PENDING_MOVE_TO_INBOX -> {
          binding.imageViewStatus.visible()
          binding.imageViewStatus.setBackgroundResource(R.drawable.ic_move_to_inbox_blue_16dp)
        }

        else -> binding.imageViewStatus.gone()
      }
    }

    private fun changeViewsTypeface(typeface: Int) {
      binding.textViewSenderAddress.setTypeface(null, typeface)
      binding.textViewDate.setTypeface(null, typeface)
    }
  }

  interface OnMessageClickListener {
    fun onMsgClick(msgEntity: MessageEntity)
  }

  companion object {
    private val ITEM_CALLBACK = object : DiffUtil.ItemCallback<MessageEntity>() {
      override fun areItemsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg.id == newMsg.id

      override fun areContentsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg == newMsg
    }
  }
}
