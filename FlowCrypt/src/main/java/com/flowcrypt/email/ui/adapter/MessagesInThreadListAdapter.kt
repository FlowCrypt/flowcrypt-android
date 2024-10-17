/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadCollapsedBinding
import com.flowcrypt.email.databinding.ItemMessageInThreadExpandedBinding
import com.flowcrypt.email.databinding.ItemThreadHeaderBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.extensions.visibleOrInvisible
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.VerticalSpaceMarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.color.MaterialColors
import kotlinx.parcelize.Parcelize
import java.nio.charset.StandardCharsets

/**
 * @author Denys Bondarenko
 */
class MessagesInThreadListAdapter(private val onMessageActionsListener: OnMessageActionsListener) :
  ListAdapter<MessagesInThreadListAdapter.Item, MessagesInThreadListAdapter.BaseViewHolder>(
    DIFF_UTIL_ITEM_CALLBACK
  ) {

  /**
   * A cache of the last content height of WebView. It will help to prevent the content blinking
   */
  private val mapWebViewHeight = mutableMapOf<Int, Int>()

  override fun getItemViewType(position: Int): Int {
    LogsUtil.d(TAG, "getItemViewType|position = $position")
    val item = getItem(position)
    return when (position) {
      0 -> Type.HEADER.id
      else -> if (item is Message) {
        item.type.id
      } else error("Unreachable")
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    LogsUtil.d(TAG, "onCreateViewHolder|viewType = $viewType")
    return when (viewType) {
      Type.HEADER.id -> HeaderViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_thread_header, parent, false),
        object : GmailApiLabelsListAdapter.OnLabelClickListener {
          override fun onLabelClick(label: GmailApiLabelsListAdapter.Label) {
            onMessageActionsListener.onLabelClicked(label)
          }
        }
      )

      Type.MESSAGE_COLLAPSED.id -> MessageCollapsedViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.item_message_in_thread_collapsed, parent, false)
      )

      Type.MESSAGE_EXPANDED.id -> MessageExpandedViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.item_message_in_thread_expanded, parent, false)
      )

      else -> error("Unreachable")
    }
  }

  override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
    LogsUtil.d(TAG, "onBindViewHolder|holder = ${holder.itemViewType}, position = $position")
    when (holder.itemViewType) {
      Type.HEADER.id -> {
        val threadHeader = (getItem(position) as? ThreadHeader) ?: return
        (holder as? HeaderViewHolder)?.bindTo(threadHeader)
      }

      Type.MESSAGE_COLLAPSED.id -> {
        val message = (getItem(position) as? Message) ?: return
        (holder as? MessageCollapsedViewHolder)?.bindTo(
          position = position,
          message = message,
          onMessageActionsListener = onMessageActionsListener
        )
      }

      Type.MESSAGE_EXPANDED.id -> {
        val message = (getItem(position) as? Message) ?: return
        (holder as? MessageExpandedViewHolder)?.bindTo(
          position = position,
          message = message,
          onMessageActionsListener = onMessageActionsListener
        )
      }

      else -> error("Unreachable")
    }
  }

  // TODO:denbond7 need to check it with long content
  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    LogsUtil.d(
      TAG,
      "onViewRecycled|holder = ${holder.itemViewType}, bindingAdapterPosition = ${holder.bindingAdapterPosition}"
    )
    //try to cache the last content height of WebView. It will help to prevent the content blinking
    if (holder is MessageExpandedViewHolder) {
      val position = holder.bindingAdapterPosition
      val holderWebViewHeight = holder.binding.emailWebView.height
      if (holderWebViewHeight != 0) {
        mapWebViewHeight[position] = holderWebViewHeight
      }
    }
  }

  override fun submitList(list: List<Item>?) {
    super.submitList(list)
    LogsUtil.d(
      TAG,
      "submitList|list.size = ${list?.size}"
    )
  }

  fun deleteMessageById(uniqueMessageId: Long) {
    val item = currentList.firstOrNull { it.id == uniqueMessageId } ?: return
    submitList(currentList - item)
  }

  interface OnMessageActionsListener {
    fun onMessageClick(position: Int, message: Message)
    fun onHeadersDetailsClick(position: Int, message: Message)
    fun onMessageChanged(position: Int, message: Message)
    fun onLabelClicked(label: GmailApiLabelsListAdapter.Label)
    fun onAttachmentDownloadClick(attachmentInfo: AttachmentInfo, message: Message)
    fun onAttachmentPreviewClick(attachmentInfo: AttachmentInfo, message: Message)
    fun onReply(message: Message)
    fun onReplyAll(message: Message)
    fun onForward(message: Message)
    fun onEditDraft(message: Message)
    fun onDeleteDraft(message: Message)
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    protected val context: Context
      get() = itemView.context
  }

  inner class HeaderViewHolder(
    itemView: View,
    listener: GmailApiLabelsListAdapter.OnLabelClickListener
  ) : BaseViewHolder(itemView) {
    val binding = ItemThreadHeaderBinding.bind(itemView)
    private val gmailApiLabelsListAdapter = GmailApiLabelsListAdapter(listener)

    fun bindTo(threadHeader: ThreadHeader) {
      binding.textViewSubject.text = threadHeader.subject

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

      gmailApiLabelsListAdapter.submitList(threadHeader.labels)
    }
  }

  inner class MessageCollapsedViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemMessageInThreadCollapsedBinding.bind(itemView)

    fun bindTo(
      position: Int,
      message: Message,
      onMessageActionsListener: OnMessageActionsListener
    ) {
      val messageEntity = message.messageEntity
      itemView.setOnClickListener { onMessageActionsListener.onMessageClick(position, message) }
      val senderAddress = messageEntity.generateFromText(context)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )
      binding.textViewSnippet.text = if (messageEntity.hasPgp == true) {
        context.getString(R.string.preview_is_not_available_for_messages_with_pgp)
      } else {
        messageEntity.snippet
      }
      binding.tVTo.text = messageEntity.generateToText(context)
      binding.textViewSender.apply {
        text = senderAddress
        if (messageEntity.isSeen) {
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

        text = messageEntity.appendDraftLabelIfNeeded(context, text)
      }
      binding.textViewDate.apply {
        text = DateTimeUtil.formatSameDayTime(context, messageEntity.receivedDate ?: 0)
        if (messageEntity.isSeen) {
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
      binding.viewHasPgp.visibleOrGone(messageEntity.hasPgp == true || messageEntity.isEncrypted == true)
      binding.viewHasAttachments.visibleOrGone(messageEntity.hasAttachments == true)
      binding.textViewDate.setTypeface(
        null,
        if (messageEntity.isSeen) Typeface.NORMAL else Typeface.BOLD
      )
    }
  }

  inner class MessageExpandedViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemMessageInThreadExpandedBinding.bind(itemView)

    private val messageHeadersListAdapter = MessageHeadersListAdapter()
    private val pgpBadgeListAdapter = PgpBadgeListAdapter()

    init {
      binding.rVMsgDetails.apply {
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(
          VerticalSpaceMarginItemDecoration(
            marginTop = 0,
            marginBottom = 0,
            marginInternal = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
          )
        )
        adapter = messageHeadersListAdapter
      }

      binding.rVPgpBadges.apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        addItemDecoration(
          MarginItemDecoration(
            marginRight = resources.getDimensionPixelSize(R.dimen.default_margin_small)
          )
        )
        adapter = pgpBadgeListAdapter
      }

      binding.rVAttachments.apply {
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(
          MarginItemDecoration(
            marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
          )
        )
      }
    }

    fun bindTo(
      position: Int,
      message: Message,
      onMessageActionsListener: OnMessageActionsListener
    ) {
      binding.layoutHeader.setOnClickListener {
        onMessageActionsListener.onMessageClick(
          position,
          message
        )
      }

      //temporary disabled ability to edit a draft from a thread
      binding.imageButtonEditDraft.visibleOrGone(false)
      binding.imageButtonEditDraft.setOnClickListener {
        onMessageActionsListener.onEditDraft(message)
      }
      binding.imageButtonDeleteDraft.visibleOrGone(message.messageEntity.isDraft)
      binding.imageButtonDeleteDraft.setOnClickListener {
        onMessageActionsListener.onDeleteDraft(message)
      }
      binding.imageButtonReplyAll.visibleOrInvisible(!message.messageEntity.isDraft)
      binding.imageButtonMoreOptions.visibleOrInvisible(!message.messageEntity.isDraft)

      binding.rVMsgDetails.visibleOrGone(message.isHeadersDetailsExpanded)
      binding.textViewDate.visibleOrInvisible(!message.isHeadersDetailsExpanded)

      binding.iBShowDetails.apply {
        setImageResource(
          if (message.isHeadersDetailsExpanded) {
            R.drawable.ic_arrow_drop_up
          } else {
            R.drawable.ic_arrow_drop_down
          }
        )

        setOnClickListener {
          onMessageActionsListener.onHeadersDetailsClick(position, message)
        }
      }

      binding.imageButtonReplyAll.setOnClickListener {
        onMessageActionsListener.onReplyAll(message)
      }

      val messageEntity = message.messageEntity
      val senderAddress = messageEntity.generateFromText(context)
      binding.textViewSenderAddress.text =
        messageEntity.appendDraftLabelIfNeeded(context, senderAddress)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )

      binding.tVTo.text = messageEntity.generateToText(context)

      if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
        binding.textViewDate.text =
          DateTimeUtil.formatSameDayTime(context, messageEntity.sentDate ?: 0)
      } else {
        binding.textViewDate.text =
          DateTimeUtil.formatSameDayTime(context, messageEntity.receivedDate ?: 0)
      }

      binding.imageButtonReplyAll.apply {
        val colorStateList =
          ColorStateList.valueOf(
            ContextCompat.getColor(
              context, if (messageEntity.hasPgp == true) {
                R.color.colorPrimary
              } else {
                R.color.red
              }
            )
          )
        imageTintList = colorStateList
      }

      messageHeadersListAdapter.submitList(messageEntity.generateDetailsHeaders(context))

      binding.rVAttachments.adapter = AttachmentsRecyclerViewAdapter(
        isDeleteEnabled = false,
        attachmentActionListener = object :
          AttachmentsRecyclerViewAdapter.AttachmentActionListener {
          override fun onDownloadClick(attachmentInfo: AttachmentInfo) {
            onMessageActionsListener.onAttachmentDownloadClick(attachmentInfo, message)
          }

          override fun onAttachmentClick(attachmentInfo: AttachmentInfo) {}

          override fun onPreviewClick(attachmentInfo: AttachmentInfo) {
            onMessageActionsListener.onAttachmentPreviewClick(attachmentInfo, message)
          }

          override fun onDeleteClick(attachmentInfo: AttachmentInfo) {}
        }
      ).apply {
        submitList(message.attachments)
      }

      binding.emailWebView.apply {
        updateLayoutParams {
          height = mapWebViewHeight[position] ?: LayoutParams.WRAP_CONTENT
        }
      }

      if (message.incomingMessageInfo != null) {
        updateMsgView(message.incomingMessageInfo)
        updatePgpBadges(message.incomingMessageInfo)
      }

      binding.imageButtonMoreOptions.setOnClickListener {
        PopupMenu(context, binding.imageButtonMoreOptions).apply {
          menuInflater.inflate(R.menu.popup_reply_actions, menu)
          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.menuActionReply -> {
                onMessageActionsListener.onReply(message)
                true
              }

              R.id.menuActionForward -> {
                onMessageActionsListener.onForward(message)
                true
              }

              else -> {
                true
              }
            }
          }
          show()
        }
      }
    }

    private fun updatePgpBadges(incomingMessageInfo: IncomingMessageInfo) {
      val badges = mutableListOf<PgpBadgeListAdapter.PgpBadge>()

      if (incomingMessageInfo.encryptionType == MessageEncryptionType.ENCRYPTED) {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.ENCRYPTED))
      } else {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED))
      }

      val verificationResult = incomingMessageInfo.verificationResult
      if (verificationResult.hasSignedParts) {
        val badge = when {
          verificationResult.hasBadSignatures -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.BAD_SIGNATURE)
          }

          verificationResult.hasUnverifiedSignatures -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.CAN_NOT_VERIFY_SIGNATURE)
          }

          verificationResult.isPartialSigned -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.ONLY_PARTIALLY_SIGNED)
          }

          !verificationResult.isPartialSigned && verificationResult.hasMixedSignatures -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.MIXED_SIGNED)
          }

          else -> PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.SIGNED)
        }
        badges.add(badge)
      } else {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED))
      }
      pgpBadgeListAdapter.submitList(badges)
    }

    private fun updateMsgView(msgInfo: IncomingMessageInfo) {
      val inlineEncryptedAtts = mutableListOf<AttachmentInfo>()
      binding.emailWebView.loadUrl("about:blank")
      binding.layoutMessageParts.removeAllViews()
      binding.layoutSecurityWarnings.removeAllViews()

      var isFirstMsgPartText = true
      var isHtmlDisplayed = false

      for (block in msgInfo.msgBlocks ?: emptyList()) {
        val layoutInflater = LayoutInflater.from(context)
        when (block.type) {
          MsgBlock.Type.SECURITY_WARNING -> {

          }

          MsgBlock.Type.DECRYPTED_HTML, MsgBlock.Type.PLAIN_HTML -> {
            if (!isHtmlDisplayed) {
              setupWebView(block)
              isHtmlDisplayed = true
            }
          }


          MsgBlock.Type.ENCRYPTED_SUBJECT -> {}// we should skip such blocks here

          else -> {}
        }
        isFirstMsgPartText = false
      }
    }

    private fun setupWebView(block: MsgBlock) {
      binding.emailWebView.configure()

      val text = clipLargeText(block.content) ?: ""

      binding.emailWebView.loadDataWithBaseURL(
        null,
        text,
        "text/html",
        StandardCharsets.UTF_8.displayName(),
        null
      )
    }

    private fun clipLargeText(text: String?): String? {
      text?.let {
        return if (it.length > 50000) {
          it.take(50000) + "\n\n" + context.getString(R.string.clipped_message_too_large)
        } else text
      }

      return text
    }
  }


  abstract class Item {
    abstract val type: Type
    abstract val id: Long
    abstract fun areContentsTheSame(other: Any?): Boolean
  }

  @Parcelize
  data class Message(
    val messageEntity: MessageEntity,
    override val type: Type,
    val isHeadersDetailsExpanded: Boolean,
    val attachments: List<AttachmentInfo>,
    val incomingMessageInfo: IncomingMessageInfo? = null
  ) : Item(), Parcelable {
    override val id: Long
      get() = messageEntity.uid

    override fun areContentsTheSame(other: Any?): Boolean {
      return this == other
    }
  }

  data class ThreadHeader(
    val subject: String? = null,
    val labels: List<GmailApiLabelsListAdapter.Label>
  ) : Item() {
    override val type: Type = Type.HEADER
    override val id: Long = type.id.toLong()

    override fun areContentsTheSame(other: Any?): Boolean {
      return this == other
    }
  }

  @Parcelize
  enum class Type(val id: Int) : Parcelable {
    HEADER(0), MESSAGE_COLLAPSED(1), MESSAGE_EXPANDED(2),
  }

  companion object {
    private val DIFF_UTIL_ITEM_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
      override fun areItemsTheSame(oldItem: Item, newItem: Item) =
        oldItem.id == newItem.id

      override fun areContentsTheSame(oldItem: Item, newItem: Item) =
        oldItem.areContentsTheSame(newItem)
    }

    private const val TAG = "MessagesInThreadListAdapter"
  }
}