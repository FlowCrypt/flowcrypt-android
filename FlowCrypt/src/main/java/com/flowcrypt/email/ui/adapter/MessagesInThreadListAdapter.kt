/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Parcelable
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.ColorRes
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
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.SecurityWarningMsgBlock
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.ItemMessageInThreadCollapsedBinding
import com.flowcrypt.email.databinding.ItemMessageInThreadExpandedBinding
import com.flowcrypt.email.databinding.ItemThreadHeaderBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.kotlin.clip
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.extensions.visibleOrInvisible
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.VerticalSpaceMarginItemDecoration
import com.flowcrypt.email.ui.widget.EmailWebView
import com.flowcrypt.email.ui.widget.TileDrawable
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
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
class MessagesInThreadListAdapter(
  private val adapterListener: AdapterListener,
  private val onMessageActionsListener: OnMessageActionsListener
) :
  ListAdapter<MessagesInThreadListAdapter.Item, MessagesInThreadListAdapter.BaseViewHolder>(
    DIFF_UTIL_ITEM_CALLBACK
  ) {

  /**
   * A cache of the last content height of WebView. It will help to prevent the content blinking
   */
  private val mapWebViewHeight = mutableMapOf<Long, Int>()
  private val mapWebViewExpandedStates = mutableMapOf<Long, Boolean>()

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    return when (position) {
      0 -> Type.HEADER.id
      else -> if (item is Message) {
        item.type.id
      } else error("Unreachable")
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
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

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    //try to cache the last content height of WebView. It will help to prevent the content blinking
    if (holder is MessageExpandedViewHolder) {
      val position = holder.bindingAdapterPosition
      val message = (getItem(position) as? Message) ?: return
      val holderWebViewHeight = holder.binding.emailWebView.height
      if (holderWebViewHeight != 0) {
        mapWebViewHeight[message.id] = holderWebViewHeight
        mapWebViewExpandedStates[message.id] = holder.binding.emailWebView.isContentExpanded
      }
    }
  }

  override fun submitList(list: List<Item>?) {
    super.submitList(list)
    adapterListener.onDataSubmitted(list)
  }

  fun getMessageItemById(messageId: Long): Message? {
    return currentList.firstOrNull {
      it.id == messageId
    } as? Message
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
    fun addRecipientsBasedOnPgpKeyDetails(pgpKeyRingDetails: PgpKeyRingDetails)
    fun updateExistingPubKey(publicKeyEntity: PublicKeyEntity, pgpKeyRingDetails: PgpKeyRingDetails)
    fun importAdditionalPrivateKeys(message: Message)
    fun fixMissingPassphraseIssue(message: Message, fingerprints: List<String>)
    fun showSendersPublicKeyDialog(message: Message)
    fun getAccount(): AccountEntity?
  }

  interface AdapterListener {
    fun onDataSubmitted(list: List<Item>?)
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

    init {
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
    }

    fun bindTo(threadHeader: ThreadHeader) {
      binding.textViewSubject.text = threadHeader.subject
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
      itemView.setOnClickListener {
        onMessageActionsListener.onMessageClick(position, message)
        if (message.incomingMessageInfo == null) {
          mapWebViewHeight.remove(message.id)
        }
      }
      val senderAddress = messageEntity.generateFromText(context)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )
      binding.textViewSnippet.text = messageEntity.snippet
      binding.tVTo.text = messageEntity.generateToText(context)
      binding.textViewSender.apply {
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

        text = messageEntity.appendDraftLabelIfNeeded(context, senderAddress)
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
        if (messageEntity.isSeen) {
          Typeface.NORMAL
        } else {
          Typeface.BOLD
        }
      )
    }
  }

  inner class MessageExpandedViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemMessageInThreadExpandedBinding.bind(itemView)

    private val messageHeadersListAdapter = MessageHeadersListAdapter()
    private val pgpBadgeListAdapter = PgpBadgeListAdapter()

    init {
      initSomeRecyclerViews()
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

      binding.imageButtonEditDraft.apply {
        visibleOrGone(message.messageEntity.isDraft && !message.hasActiveDraftUploadingProcess)
        setOnClickListener {
          onMessageActionsListener.onEditDraft(message)
        }
      }

      binding.imageButtonDeleteDraft.apply {
        visibleOrGone(message.messageEntity.isDraft && !message.hasActiveDraftUploadingProcess)
        setOnClickListener {
          onMessageActionsListener.onDeleteDraft(message)
        }
      }

      binding.progressBarSavingDraft.apply {
        visibleOrGone(message.messageEntity.isDraft && message.hasActiveDraftUploadingProcess)
      }

      binding.imageButtonReplyAll.apply {
        visibleOrInvisible(!message.messageEntity.isDraft)
        setOnClickListener {
          onMessageActionsListener.onReplyAll(message)
        }
      }

      binding.imageButtonMoreOptions.apply {
        visibleOrInvisible(!message.messageEntity.isDraft)
        setOnClickListener {
          showPopupForMoreOptionButton(onMessageActionsListener, message)
        }
      }

      binding.textViewDate.apply {
        visibleOrInvisible(!message.isHeadersDetailsExpanded)
        val isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equals(
          message.messageEntity.folder,
          ignoreCase = true
        )
        text = if (isOutbox) {
          DateTimeUtil.formatSameDayTime(context, message.messageEntity.sentDate ?: 0)
        } else {
          DateTimeUtil.formatSameDayTime(context, message.messageEntity.receivedDate ?: 0)
        }
      }

      binding.rVMsgDetails.visibleOrGone(message.isHeadersDetailsExpanded)

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

      val senderAddress = message.messageEntity.generateFromText(context)
      binding.textViewSenderAddress.text =
        message.messageEntity.appendDraftLabelIfNeeded(context, senderAddress)
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      )

      binding.tVTo.text = message.messageEntity.generateToText(context)

      binding.imageButtonReplyAll.apply {
        imageTintList = ColorStateList.valueOf(
          ContextCompat.getColor(
            context, if (message.messageEntity.hasPgp == true) {
              R.color.colorPrimary
            } else {
              R.color.red
            }
          )
        )
      }

      messageHeadersListAdapter.submitList(message.messageEntity.generateDetailsHeaders(context))

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
        submitList(message.attachments.filterNot { it.isHidden() })
      }

      binding.emailWebView.apply {
        updateLayoutParams {
          //this code prevents content blinking
          height = mapWebViewHeight[message.id]?.takeIf { it > 0 } ?: LayoutParams.WRAP_CONTENT
        }
      }

      if (message.incomingMessageInfo != null) {
        updateMsgView(message, onMessageActionsListener)
        updatePgpBadges(message)
      }
    }

    private fun showPopupForMoreOptionButton(
      onMessageActionsListener: OnMessageActionsListener,
      message: Message
    ) {
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

    private fun updatePgpBadges(message: Message) {
      val badges = mutableListOf<PgpBadgeListAdapter.PgpBadge>()
      val incomingMessageInfo: IncomingMessageInfo = message.incomingMessageInfo ?: return

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
            if (message.hasActiveSignatureVerification) {
              PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.VERIFYING_SIGNATURE)
            } else {
              PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.CAN_NOT_VERIFY_SIGNATURE)
            }
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

    private fun updateMsgView(
      message: Message,
      onMessageActionsListener: OnMessageActionsListener
    ) {
      val msgInfo = message.incomingMessageInfo ?: return
      binding.emailWebView.loadUrl("about:blank")
      binding.layoutMessageParts.removeAllViews()
      binding.layoutSecurityWarnings.removeAllViews()

      var isHtmlDisplayed = false

      for (block in msgInfo.msgBlocks ?: emptyList()) {
        val layoutInflater = LayoutInflater.from(context)
        when (block.type) {
          MsgBlock.Type.SECURITY_WARNING -> {
            if (block is SecurityWarningMsgBlock) {
              when (block.warningType) {
                SecurityWarningMsgBlock.WarningType.RECEIVED_SPF_SOFT_FAIL -> {
                  binding.layoutSecurityWarnings.addView(
                    getView(
                      originalMsg = block.content?.clip(context, TEXT_MAX_SIZE),
                      errorMsg = context.getText(R.string.spf_soft_fail_warning),
                      layoutInflater = layoutInflater,
                      leftBorderColor = R.color.orange
                    )
                  )
                }
              }
            }
          }

          MsgBlock.Type.DECRYPTED_HTML, MsgBlock.Type.PLAIN_HTML -> {
            if (!isHtmlDisplayed) {
              setupWebView(message, block)
              isHtmlDisplayed = true
            }
          }

          MsgBlock.Type.PLAIN_TEXT -> {
            binding.layoutMessageParts.addView(genTextPart(block, layoutInflater))
          }

          MsgBlock.Type.PUBLIC_KEY ->
            binding.layoutMessageParts.addView(
              genPublicKeyPart(
                block as PublicKeyMsgBlock,
                layoutInflater,
                onMessageActionsListener
              )
            )

          MsgBlock.Type.DECRYPT_ERROR -> {
            binding.layoutMessageParts.addView(
              genDecryptErrorPart(
                message,
                block as DecryptErrorMsgBlock,
                layoutInflater
              )
            )
          }

          MsgBlock.Type.DECRYPTED_ATT -> {
            val decryptAtt = block as? DecryptedAttMsgBlock
            if (decryptAtt == null) {
              handleOtherBlock(block, layoutInflater)
            }
          }

          MsgBlock.Type.ENCRYPTED_SUBJECT, MsgBlock.Type.INLINE_ATT -> {}// we should skip such blocks here

          else -> handleOtherBlock(block, layoutInflater)
        }
      }
    }

    private fun setupWebView(message: Message, block: MsgBlock) {
      binding.emailWebView.configure()

      val shouldBeExpandedIfPossible = mapWebViewExpandedStates[message.id] ?: false
      val text = block.content?.let {
        if (shouldBeExpandedIfPossible) it.replaceFirst(
          "<details>",
          "<details open>"
        ) else it
      }?.clip(context, TEXT_MAX_SIZE) ?: ""

      binding.emailWebView.loadDataWithBaseURL(
        null,
        text,
        "text/html",
        StandardCharsets.UTF_8.displayName(),
        null
      )

      binding.emailWebView.setOnPageLoadingListener(object : EmailWebView.OnPageLoadingListener {
        override fun onPageLoading(newProgress: Int) {
          if (newProgress >= 100) {
            //to prevent wrong WebView size need to back using LayoutParams.WRAP_CONTENT
            binding.emailWebView.apply {
              updateLayoutParams { height = LayoutParams.WRAP_CONTENT }
            }
          }
        }
      })
    }

    private fun genPublicKeyPart(
      block: PublicKeyMsgBlock,
      inflater: LayoutInflater,
      onMessageActionsListener: OnMessageActionsListener
    ): View {
      if (block.error?.errorMsg?.isNotEmpty() == true) {
        return getView(
          block.content?.clip(context, TEXT_MAX_SIZE),
          context.getString(R.string.msg_contains_not_valid_pub_key, block.error.errorMsg),
          inflater
        )
      }

      val pubKeyView = inflater.inflate(
        R.layout.message_part_public_key,
        binding.layoutMessageParts,
        false
      ) as ViewGroup
      val textViewPgpPublicKey = pubKeyView.findViewById<TextView>(R.id.textViewPgpPublicKey)
      val switchShowPublicKey = pubKeyView.findViewById<CompoundButton>(R.id.switchShowPublicKey)

      switchShowPublicKey.setOnCheckedChangeListener { buttonView, isChecked ->
        TransitionManager.beginDelayedTransition(pubKeyView)
        textViewPgpPublicKey.visibility = if (isChecked) View.VISIBLE else View.GONE
        buttonView.setText(if (isChecked) R.string.hide_the_public_key else R.string.show_the_public_key)
      }

      val keyDetails = block.keyDetails
      val userIds = keyDetails?.getUserIdsAsSingleString()

      if (userIds?.isNotEmpty() == true) {
        val keyOwner = pubKeyView.findViewById<TextView>(R.id.textViewKeyOwnerTemplate)
        keyOwner.text = context.getString(R.string.template_message_part_public_key_owner, userIds)
      }

      val fingerprint = pubKeyView.findViewById<TextView>(R.id.textViewFingerprintTemplate)
      UIUtil.setHtmlTextToTextView(
        context.getString(
          R.string.template_message_part_public_key_fingerprint,
          GeneralUtil.doSectionsInText(" ", keyDetails?.fingerprint, 4)
        ), fingerprint
      )

      textViewPgpPublicKey.text =
        (block.keyDetails?.publicKey ?: block.content)?.clip(context, TEXT_MAX_SIZE)

      val existingRecipientWithPubKeys = block.existingRecipientWithPubKeys
      val button = pubKeyView.findViewById<Button>(R.id.buttonKeyAction)
      val textViewStatus = pubKeyView.findViewById<TextView>(R.id.textViewStatus)
      val textViewManualImportWarning =
        pubKeyView.findViewById<TextView>(R.id.textViewManualImportWarning)
      if (keyDetails?.usableForEncryption == true) {
        if (existingRecipientWithPubKeys == null) {
          button?.apply {
            setText(R.string.import_pub_key)
            setOnClickListener {
              onMessageActionsListener.addRecipientsBasedOnPgpKeyDetails(keyDetails)
              gone()
              textViewManualImportWarning.gone()
              textViewStatus?.apply {
                text = context.getString(R.string.already_imported)
                visible()
              }
            }
          }
        } else {
          val matchingKeyByFingerprint = existingRecipientWithPubKeys.publicKeys.firstOrNull {
            it.fingerprint.equals(keyDetails.fingerprint, true)
          }
          if (matchingKeyByFingerprint != null) {
            when {
              matchingKeyByFingerprint.pgpKeyRingDetails?.isRevoked == true -> {
                textViewStatus?.text = context.getString(R.string.key_is_revoked_unable_to_update)
                textViewStatus?.setTextColor(UIUtil.getColor(context, R.color.red))
                textViewStatus?.visible()
                textViewManualImportWarning?.gone()
                button?.gone()
              }

              keyDetails.isNewerThan(matchingKeyByFingerprint.pgpKeyRingDetails) -> {
                textViewManualImportWarning?.visible()
                button?.apply {
                  setText(R.string.update_pub_key)
                  setOnClickListener {
                    onMessageActionsListener.updateExistingPubKey(
                      matchingKeyByFingerprint,
                      keyDetails
                    )
                    gone()
                  }
                }
              }

              else -> {
                textViewStatus?.text = context.getString(R.string.already_imported)
                textViewStatus.visible()
                textViewManualImportWarning?.gone()
                button?.gone()
              }
            }
          } else {
            button?.gone()
          }
        }
      } else {
        textViewStatus.text = context.getString(R.string.cannot_be_used_for_encryption)
        textViewStatus.setTextColor(UIUtil.getColor(context, R.color.red))
        textViewManualImportWarning?.gone()
        button?.gone()
        textViewStatus.visible()
      }

      return pubKeyView
    }

    private fun genDecryptErrorPart(
      message: Message,
      block: DecryptErrorMsgBlock,
      layoutInflater: LayoutInflater
    ): View {
      val decryptError = block.decryptErr ?: return View(context)

      when (decryptError.details?.type) {
        PgpDecryptAndOrVerify.DecryptionErrorType.KEY_MISMATCH ->
        return generateMissingPrivateKeyLayout(
          message = message,
          pgpMsg = block.content?.clip(context, TEXT_MAX_SIZE),
          inflater = layoutInflater,
          onMessageActionsListener = onMessageActionsListener
        )

        PgpDecryptAndOrVerify.DecryptionErrorType.FORMAT -> {
          val formatErrorMsg = (context.getString(
            R.string.decrypt_error_message_badly_formatted,
            context.getString(R.string.app_name)
          ) + "\n\n"
              + decryptError.details.type + ": " + decryptError.details.message)
          return getView(
            block.content?.clip(context, TEXT_MAX_SIZE),
            formatErrorMsg,
            layoutInflater
          )
        }

        PgpDecryptAndOrVerify.DecryptionErrorType.OTHER -> {
          val otherErrorMsg = when (decryptError.details.message) {
            "Symmetric-Key algorithm TRIPLE_DES is not acceptable for message decryption." -> {
              context.getString(R.string.message_was_not_decrypted_due_to_triple_des, "TRIPLE_DES")
            }

            else -> context.getString(
              R.string.decrypt_error_could_not_open_message,
              context.getString(R.string.app_name)
            ) + "\n\n" + context.getString(
              R.string.decrypt_error_please_write_me,
              context.getString(R.string.support_email)
            ) + "\n\n" + decryptError.details.type +
                ": " + decryptError.details.message +
                "\n\n" + decryptError.details.stack
          }

          return getView(block.content?.clip(context, TEXT_MAX_SIZE), otherErrorMsg, layoutInflater)
        }

        else -> {
          var btText: String? = null
          var onClickListener: View.OnClickListener? = null
          if (decryptError.details?.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
            btText = context.getString(R.string.fix)
            onClickListener = View.OnClickListener {
              val fingerprints = decryptError.fingerprints ?: return@OnClickListener
              onMessageActionsListener.fixMissingPassphraseIssue(message, fingerprints)
            }
          }

          val detailedMessage = when (decryptError.details?.type) {
            PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC -> {
              context.getString(R.string.decrypt_error_message_no_mdc)
            }

            PgpDecryptAndOrVerify.DecryptionErrorType.BAD_MDC -> {
              context.getString(R.string.decrypt_error_message_bad_mdc)
            }

            else -> decryptError.details?.message
          }

          return getView(
            originalMsg = block.content?.clip(context, TEXT_MAX_SIZE),
            errorMsg = context.getString(
              R.string.could_not_decrypt_message_due_to_error,
              decryptError.details?.type.toString() + ": " + detailedMessage
            ),
            layoutInflater = layoutInflater,
            buttonText = btText,
            onClickListener = onClickListener
          )
        }
      }
    }

    private fun getView(
      originalMsg: String?,
      errorMsg: CharSequence,
      layoutInflater: LayoutInflater,
      buttonText: String? = null,
      @ColorRes
      leftBorderColor: Int = R.color.red,
      onClickListener: View.OnClickListener? = null
    ): View {
      val viewGroup = layoutInflater.inflate(
        R.layout.message_part_pgp_message_error,
        binding.layoutMessageParts, false
      ) as ViewGroup
      val container = viewGroup.findViewById<ViewGroup>(R.id.container)
      val existingBackground = container.background as LayerDrawable

      //use SVG as a repeatable drawable
      val drawable =
        ContextCompat.getDrawable(context, R.drawable.bg_security_repeat_template)
      if (drawable != null && existingBackground.numberOfLayers > 1) {
        existingBackground.setDrawable(1, TileDrawable(drawable, Shader.TileMode.REPEAT))
      }

      val gradientDrawable = existingBackground.getDrawable(3)
      if (gradientDrawable is GradientDrawable) {
        gradientDrawable.setColor(ContextCompat.getColor(context, leftBorderColor))
      }

      val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
      textViewErrorMsg.text = errorMsg
      if (originalMsg != null) {
        viewGroup.addView(genShowOrigMsgLayout(originalMsg, layoutInflater, viewGroup))
        onClickListener?.let {
          val btAction = viewGroup.findViewById<TextView>(R.id.btAction)
          btAction.text = buttonText
          btAction.visible()
          btAction.setOnClickListener(it)
        }
      }
      return viewGroup
    }

    /**
     * Generate a layout with switch button which will be regulate visibility of original message info.
     *
     * @param msg            The original pgp message info.
     * @param layoutInflater The [LayoutInflater] instance.
     * @param rootView       The root view which will be used while we create a new layout using
     * [LayoutInflater].
     * @return A generated layout.
     */
    private fun genShowOrigMsgLayout(
      msg: String?, layoutInflater: LayoutInflater,
      rootView: ViewGroup
    ): ViewGroup {
      val viewGroup =
        layoutInflater.inflate(R.layout.pgp_show_original_message, rootView, false) as ViewGroup
      val textViewOrigPgpMsg = viewGroup.findViewById<TextView>(R.id.textViewOrigPgpMsg)
      textViewOrigPgpMsg.text = msg

      val switchShowOrigMsg = viewGroup.findViewById<CompoundButton>(R.id.switchShowOrigMsg)

      switchShowOrigMsg.setOnCheckedChangeListener { buttonView, isChecked ->
        TransitionManager.beginDelayedTransition(rootView)
        textViewOrigPgpMsg.visibility = if (isChecked) View.VISIBLE else View.GONE
        buttonView.setText(if (isChecked) R.string.hide_original_message else R.string.show_original_message)
      }
      return viewGroup
    }

    private fun genTextPart(block: MsgBlock, layoutInflater: LayoutInflater): View {
      return genDefPart(
        block,
        layoutInflater,
        R.layout.message_part_text,
        binding.layoutMessageParts
      )
    }

    private fun genDefPart(
      block: MsgBlock,
      inflater: LayoutInflater,
      res: Int,
      viewGroup: ViewGroup?
    ): View {
      val errorMsg = block.error?.errorMsg
      return if (errorMsg?.isNotEmpty() == true) {
        getView(
          null,
          context.getString(R.string.msg_contains_not_valid_block, block.type.toString(), errorMsg),
          inflater
        )
      } else {
        val textViewMsgPartOther = inflater.inflate(res, viewGroup, false) as TextView
        textViewMsgPartOther.text = block.content?.clip(context, TEXT_MAX_SIZE)
        textViewMsgPartOther
      }
    }

    private fun handleOtherBlock(
      block: @JvmSuppressWildcards MsgBlock,
      layoutInflater: LayoutInflater
    ) {
      binding.layoutMessageParts.addView(
        genDefPart(
          block, layoutInflater,
          R.layout.message_part_other, binding.layoutMessageParts
        )
      )
    }

    /**
     * Generate a layout which describes the missing private keys situation.
     *
     * @param pgpMsg   The pgp message.
     * @param inflater The [LayoutInflater] instance.
     * @return Generated layout.
     */
    private fun generateMissingPrivateKeyLayout(
      message: Message,
      pgpMsg: String?,
      inflater: LayoutInflater,
      onMessageActionsListener: OnMessageActionsListener
    ): View {
      val viewGroup = inflater.inflate(
        R.layout.message_part_pgp_message_missing_private_key, binding.layoutMessageParts, false
      ) as ViewGroup
      val buttonImportPrivateKey = viewGroup.findViewById<Button>(R.id.buttonImportPrivateKey)
      buttonImportPrivateKey?.setOnClickListener {
        onMessageActionsListener.importAdditionalPrivateKeys(message)
      }

      val buttonSendOwnPublicKey = viewGroup.findViewById<Button>(R.id.buttonSendOwnPublicKey)
      buttonSendOwnPublicKey?.setOnClickListener {
        onMessageActionsListener.showSendersPublicKeyDialog(message)
      }

      val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
      if (onMessageActionsListener.getAccount()?.clientConfiguration?.usesKeyManager() == true) {
        textViewErrorMsg?.text = context.getString(R.string.your_keys_cannot_open_this_message)
        buttonImportPrivateKey?.gone()
        buttonSendOwnPublicKey?.text = context.getString(R.string.inform_sender)
      } else {
        textViewErrorMsg?.text =
          context.getString(R.string.decrypt_error_current_key_cannot_open_message)
      }

      viewGroup.addView(genShowOrigMsgLayout(pgpMsg, inflater, viewGroup))
      return viewGroup
    }

    private fun initSomeRecyclerViews() {
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
    val incomingMessageInfo: IncomingMessageInfo? = null,
    val hasActiveSignatureVerification: Boolean = false,
    val hasActiveDraftUploadingProcess: Boolean = false
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

    private const val TEXT_MAX_SIZE = 50000
  }
}