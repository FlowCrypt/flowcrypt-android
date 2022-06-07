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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.UIUtil
import jakarta.mail.internet.InternetAddress
import java.util.regex.Pattern

/**
 * This class is responsible for displaying the message in the list.
 *
 * @author Denis Bondarenko
 *         Date: 12/15/19
 *         Time: 4:48 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgsPagedListAdapter(private val onMessageClickListener: OnMessageClickListener? = null) :
  PagedListAdapter<MessageEntity, MsgsPagedListAdapter.BaseViewHolder>(DIFF_CALLBACK) {
  private val senderNamePattern: Pattern
  var tracker: SelectionTracker<Long>? = null
  var currentFolder: LocalFolder? = null

  init {
    this.senderNamePattern = prepareSenderNamePattern()
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, @ItemType viewType: Int): BaseViewHolder {
    return when (viewType) {
      FOOTER -> object : BaseViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.list_view_progress_footer, parent, false)
      ) {
        override val itemType = FOOTER
      }

      MESSAGE -> MessageViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.messages_list_item, parent, false)
      )

      else -> object : BaseViewHolder(ProgressBar(parent.context)) {
        override val itemType = NONE
      }
    }
  }

  override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
    holder.setActivatedState(tracker?.isSelected(getItem(position)?.id) ?: false)

    when (holder.itemType) {
      MESSAGE -> {
        val msgEntity = getItem(position)
        updateItem(msgEntity, holder as MessageViewHolder)
        holder.itemView.setOnClickListener {
          msgEntity?.let { onMessageClickListener?.onMsgClick(it) }
        }
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return MESSAGE
  }

  override fun getItemId(position: Int): Long {
    return getItem(position)?.id ?: super.getItemId(position)
  }

  override fun onCurrentListChanged(
    previousList: PagedList<MessageEntity>?,
    currentList: PagedList<MessageEntity>?
  ) {
    super.onCurrentListChanged(previousList, currentList)
    val currentIds = currentList?.map { it?.id }?.toSet()
    val ids = tracker?.selection?.map { it } ?: emptyList<Long>()

    for (id in ids) {
      if (currentIds?.contains(id) == false) {
        tracker?.deselect(id)
      }
    }
  }

  fun getMsgEntity(position: Int?): MessageEntity? {
    position ?: return null

    if (position == RecyclerView.NO_POSITION) {
      return null
    }

    return getItem(position)
  }

  fun changeProgress(isFooterEnabled: Boolean) {
    LogsUtil.d(javaClass.name, isFooterEnabled.toString())
  }

  private fun updateItem(messageEntity: MessageEntity?, viewHolder: MessageViewHolder) {
    val context = viewHolder.itemView.context
    if (messageEntity != null) {
      val subject = if (TextUtils.isEmpty(messageEntity.subject)) {
        context.getString(R.string.no_subject)
      } else {
        messageEntity.subject
      }

      val folderType = FoldersManager.getFolderType(currentFolder)
      if (folderType != null) {
        when (folderType) {
          FoldersManager.FolderType.SENT -> viewHolder.textViewSenderAddress?.text =
            generateAddresses(messageEntity.to)

          FoldersManager.FolderType.OUTBOX -> {
            val status = generateOutboxStatus(
              viewHolder.textViewSenderAddress?.context,
              messageEntity.msgState
            )
            viewHolder.textViewSenderAddress?.text = status
          }

          else -> viewHolder.textViewSenderAddress?.text = generateAddresses(messageEntity.from)
        }
      } else {
        viewHolder.textViewSenderAddress?.text = generateAddresses(messageEntity.from)
      }

      viewHolder.textViewSubject?.text = subject
      if (folderType === FoldersManager.FolderType.OUTBOX) {
        viewHolder.textViewDate?.text =
          DateTimeUtil.formatSameDayTime(context, messageEntity.sentDate)
      } else {
        viewHolder.textViewDate?.text =
          DateTimeUtil.formatSameDayTime(context, messageEntity.receivedDate)
      }

      if (messageEntity.isSeen) {
        changeViewsTypeface(viewHolder, Typeface.NORMAL)
        viewHolder.textViewSenderAddress?.setTextColor(UIUtil.getColor(context, R.color.dark))
        viewHolder.textViewDate?.setTextColor(UIUtil.getColor(context, R.color.gray))
      } else {
        changeViewsTypeface(viewHolder, Typeface.BOLD)
        viewHolder.textViewSenderAddress?.setTextColor(
          UIUtil.getColor(
            context,
            android.R.color.black
          )
        )
        viewHolder.textViewDate?.setTextColor(UIUtil.getColor(context, android.R.color.black))
      }

      viewHolder.imageViewAtts?.visibility =
        if (messageEntity.hasAttachments == true) View.VISIBLE else View.GONE
      viewHolder.viewIsEncrypted?.visibility =
        if (messageEntity.isEncrypted == true) View.VISIBLE else View.GONE

      when (messageEntity.msgState) {
        MessageState.PENDING_ARCHIVING -> {
          with(viewHolder.imageViewStatus) {
            this?.visibility = View.VISIBLE
            this?.setBackgroundResource(R.drawable.ic_archive_blue_16dp)
          }
        }

        MessageState.PENDING_MARK_UNREAD -> {
          with(viewHolder.imageViewStatus) {
            this?.visibility = View.VISIBLE
            this?.setBackgroundResource(R.drawable.ic_markunread_blue_16dp)
          }
        }

        MessageState.PENDING_DELETING,
        MessageState.PENDING_DELETING_PERMANENTLY,
        MessageState.PENDING_EMPTY_TRASH -> {
          with(viewHolder.imageViewStatus) {
            this?.visibility = View.VISIBLE
            this?.setBackgroundResource(R.drawable.ic_delete_blue_16dp)
          }
        }

        MessageState.PENDING_MOVE_TO_INBOX -> {
          with(viewHolder.imageViewStatus) {
            this?.visibility = View.VISIBLE
            this?.setBackgroundResource(R.drawable.ic_move_to_inbox_blue_16dp)
          }
        }

        else -> viewHolder.imageViewStatus?.visibility = View.GONE
      }

    } else {
      clearItem(viewHolder)
    }
  }

  private fun changeViewsTypeface(viewHolder: MessageViewHolder, typeface: Int) {
    viewHolder.textViewSenderAddress?.setTypeface(null, typeface)
    viewHolder.textViewDate?.setTypeface(null, typeface)
  }

  /**
   * Prepare a [Pattern] which will be used for finding some information in the sender name. This pattern is
   * case insensitive.
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

  /**
   * Clear all views in the item.
   *
   * @param viewHolder A View holder object which consist links to views.
   */
  private fun clearItem(viewHolder: MessageViewHolder) {
    viewHolder.textViewSenderAddress?.text = null
    viewHolder.textViewSubject?.text = null
    viewHolder.textViewDate?.text = null
    viewHolder.imageViewAtts?.visibility = View.GONE
    viewHolder.viewIsEncrypted?.visibility = View.GONE
    viewHolder.imageViewStatus?.visibility = View.GONE

    changeViewsTypeface(viewHolder, Typeface.NORMAL)
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

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @ItemType
    abstract val itemType: Int

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
      object : ItemDetailsLookup.ItemDetails<Long>() {
        override fun getPosition(): Int = bindingAdapterPosition
        override fun getSelectionKey(): Long = itemId
      }

    fun setActivatedState(isActivated: Boolean) {
      itemView.isActivated = isActivated
    }
  }

  inner class MessageViewHolder(itemView: View) : BaseViewHolder(itemView) {
    override val itemType = MESSAGE

    var textViewSenderAddress: TextView? = itemView.findViewById(R.id.textViewSenderAddress)
    var textViewDate: TextView? = itemView.findViewById(R.id.textViewDate)
    var textViewSubject: TextView? = itemView.findViewById(R.id.textViewSubject)
    var imageViewAtts: ImageView? = itemView.findViewById(R.id.imageViewAtts)
    var imageViewStatus: ImageView? = itemView.findViewById(R.id.imageViewStatus)
    var viewIsEncrypted: View? = itemView.findViewById(R.id.viewIsEncrypted)
  }

  interface OnMessageClickListener {
    fun onMsgClick(msgEntity: MessageEntity)
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessageEntity>() {
      override fun areItemsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg.id == newMsg.id

      override fun areContentsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg == newMsg
    }

    @IntDef(NONE, FOOTER, MESSAGE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ItemType

    const val NONE = 0
    const val FOOTER = 1
    const val MESSAGE = 2
  }
}
