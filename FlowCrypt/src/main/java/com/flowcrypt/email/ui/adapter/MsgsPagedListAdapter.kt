/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.graphics.Camera
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.ViewAnimationFactory
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.MessagesListItemBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.color.MaterialColors
import java.util.regex.Pattern

/**
 * This class is responsible for displaying the message in the list.
 *
 * @author Denys Bondarenko
 */
class MsgsPagedListAdapter(private val onMessageClickListener: OnMessageClickListener? = null) :
  PagedListAdapter<MessageEntity, MsgsPagedListAdapter.BaseViewHolder>(DIFF_CALLBACK) {

  var tracker: SelectionTracker<Long>? = null
  var labelsEntities: List<LabelEntity>? = null
  var currentFolder: LocalFolder? = null
    set(value) {
      field = value
      folderType = value?.let { FoldersManager.getFolderType(it) }
    }

  private var folderType: FoldersManager.FolderType? = null

  private val onAvatarClickListener = object : MessageViewHolder.OnAvatarClickListener {
    override fun onAvatarClick(msgEntity: MessageEntity) {
      msgEntity.id?.let { tracker?.select(it) }
    }
  }

  init {
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
          .inflate(R.layout.messages_list_item, parent, false),
        onAvatarClickListener
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
        val messageEntity = getItem(position)
        (holder as? MessageViewHolder)?.bind(
          messageEntity,
          currentFolder?.fullName,
          folderType,
          labelsEntities
        )
        holder.itemView.setOnClickListener {
          messageEntity?.let { onMessageClickListener?.onMsgClick(position, it) }
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

  abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

  class MessageViewHolder(
    itemView: View,
    private val onAvatarClickListener: OnAvatarClickListener
  ) : BaseViewHolder(itemView) {
    private val binding: MessagesListItemBinding = MessagesListItemBinding.bind(itemView)
    override val itemType = MESSAGE
    private var lastDataId: Long? = null
    private val gmailApiLabelsListAdapter = GmailApiLabelsListAdapter(
      object : GmailApiLabelsListAdapter.OnLabelClickListener {
        override fun onLabelClick(label: GmailApiLabelsListAdapter.Label) {}
      }
    )

    init {
      binding.recyclerViewLabels.apply {
        layoutManager = FlexboxLayoutManager(itemView.context).apply {
          flexDirection = FlexDirection.ROW
          justifyContent = JustifyContent.FLEX_START
          maxLine = 1
        }
        addItemDecoration(
          MarginItemDecoration(
            marginLeft = resources.getDimensionPixelSize(R.dimen.default_margin_small)
          )
        )
        adapter = gmailApiLabelsListAdapter
        itemAnimator = null
      }
    }

    fun bind(
      messageEntity: MessageEntity?,
      folderName: String?,
      folderType: FoldersManager.FolderType?,
      labelsEntities: List<LabelEntity>?
    ) {
      val context = itemView.context
      if (messageEntity != null) {
        val subject = if (TextUtils.isEmpty(messageEntity.subject)) {
          context.getString(R.string.no_subject)
        } else {
          messageEntity.subject
        }

        val coloredLabels = MessageEntity.generateColoredLabels(
          labelIds = messageEntity.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR),
          labelEntities = labelsEntities,
          skippedLabels = listOf(folderName ?: "")
        )
        gmailApiLabelsListAdapter.submitList(
          if (coloredLabels.size > MAX_LABELS_COUNT_BE_VISIBLE) {
            coloredLabels.subList(
              0,
              MAX_LABELS_COUNT_BE_VISIBLE
            ) + listOf(GmailApiLabelsListAdapter.Label("..."))
          } else {
            coloredLabels
          }
        )

        val senderAddress = when (folderType) {
          FoldersManager.FolderType.OUTBOX -> generateOutboxStatus(context, messageEntity.msgState)

          else -> messageEntity.generateSenderAddresses(
            context,
            folderType,
            context.getString(R.string.me)
          )
        }
        binding.textViewSenderAddress.text = if (messageEntity.isGmailThread) {
          messageEntity.getThreadSpannableString(context).let { spannableString ->
            SpannableStringBuilder(senderAddress).apply {
              if (spannableString.isNotEmpty()) {
                when {
                  spannableString.startsWith("(") -> {
                    append(" ")
                  }

                  senderAddress.isNotEmpty() -> {
                    append(", ")
                  }
                }
              }
              append(spannableString)
            }
          }
        } else {
          senderAddress
        }

        updateAvatar(senderAddress, folderType, lastDataId == messageEntity.id)
        binding.imageViewAvatar.setOnClickListener {
          onAvatarClickListener.onAvatarClick(messageEntity)
        }

        binding.textViewSubject.text = subject
        if (folderType in listOf(
            FoldersManager.FolderType.OUTBOX,
            FoldersManager.FolderType.DRAFTS
          )
        ) {
          binding.textViewDate.text =
            DateTimeUtil.formatSameDayTime(context, messageEntity.sentDate)
        } else {
          binding.textViewDate.text =
            DateTimeUtil.formatSameDayTime(context, messageEntity.receivedDate)
        }

        if (messageEntity.isSeen) {
          changeViewsTypeface(Typeface.NORMAL)
          binding.textViewSenderAddress.setTextColor(
            MaterialColors.getColor(
              context,
              com.google.android.material.R.attr.colorOnSurfaceVariant,
              Color.BLACK
            )
          )
          binding.textViewDate.setTextColor(
            MaterialColors.getColor(context, R.attr.itemSubTitleColor, Color.BLACK)
          )
        } else {
          changeViewsTypeface(Typeface.BOLD)
          binding.textViewSenderAddress.setTextColor(
            MaterialColors.getColor(context, R.attr.itemTitleColor, Color.BLACK)
          )
          binding.textViewDate.setTextColor(
            MaterialColors.getColor(context, R.attr.itemTitleColor, Color.BLACK)
          )
        }

        binding.imageViewAtts.visibleOrGone(messageEntity.hasAttachments == true)
        binding.viewHasPgp.visibleOrGone(
          messageEntity.hasPgp == true || messageEntity.isEncrypted == true
        )

        changeStatusView(messageEntity)
      } else {
        clearData()
      }

      binding.recyclerViewLabels.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        setMargins(
          0,
          0,
          if (binding.viewHasPgp.isVisible || binding.imageViewStatus.isVisible) {
            itemView.context.resources.getDimension(R.dimen.default_margin_small).toInt()
          } else {
            0
          },
          0
        )
      }

      lastDataId = messageEntity?.id
    }

    private fun changeStatusView(messageEntity: MessageEntity) {
      when (messageEntity.msgState) {
        MessageState.PENDING_ARCHIVING -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_archive_blue_16dp)
          }
        }

        MessageState.PENDING_MARK_UNREAD -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_markunread_blue_16dp)
          }
        }

        MessageState.PENDING_DELETING,
        MessageState.PENDING_DELETING_PERMANENTLY,
        MessageState.PENDING_DELETING_DRAFT,
        MessageState.PENDING_EMPTY_TRASH -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_delete_blue_16dp)
          }
        }

        MessageState.PENDING_MOVE_TO_INBOX -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_move_to_inbox_blue_16dp)
          }
        }

        MessageState.PENDING_MOVE_TO_SPAM -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_to_spam_blue_16dp)
          }
        }

        MessageState.PENDING_MARK_AS_NOT_SPAM -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_from_spam_blue_16dp)
          }
        }

        MessageState.PENDING_UPLOADING_DRAFT -> {
          with(binding.imageViewStatus) {
            visibility = View.VISIBLE
            setBackgroundResource(R.drawable.ic_baseline_pending_actions_blue_24)
          }
        }

        else -> binding.imageViewStatus.gone()
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

            MessageState.ERROR_DURING_CREATION -> state =
              context.getString(R.string.could_not_create)

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

    private fun changeViewsTypeface(typeface: Int) {
      binding.textViewSenderAddress.setTypeface(null, typeface)
      binding.textViewDate.setTypeface(null, typeface)
    }

    private fun updateAvatar(
      senderAddress: CharSequence? = null,
      folderType: FoldersManager.FolderType? = null,
      useAnimationForCheckedState: Boolean = false
    ) {
      binding.imageViewAvatar.useGlideToApplyImageFromSource(
        source = when {
          itemView.isActivated -> ContextCompat.getDrawable(
            itemView.context,
            R.drawable.ic_selected
          ) ?: R.drawable.ic_selected

          folderType == FoldersManager.FolderType.DRAFTS -> R.drawable.avatar_draft
          folderType in listOf(
            FoldersManager.FolderType.SPAM,
            FoldersManager.FolderType.JUNK
          ) -> R.drawable.ic_spam_48dp

          else -> senderAddress?.let {
            AvatarModelLoader.SCHEMA_AVATAR + it
          } ?: R.drawable.ic_account_default_photo
        },
        transitionOptions = if (itemView.isActivated && useAnimationForCheckedState) {
          DrawableTransitionOptions.with(
            ViewAnimationFactory(Rotate3dAnimation.createAnimation {
              binding.imageViewAvatar.width * 0.5f
            })
          )
        } else null
      )
    }

    /**
     * Clear all views.
     */
    private fun clearData() {
      updateAvatar()

      binding.textViewSenderAddress.text = null
      binding.textViewSubject.text = null
      binding.textViewDate.text = null
      binding.imageViewAtts.gone()
      binding.viewHasPgp.gone()
      binding.imageViewStatus.gone()

      changeViewsTypeface(Typeface.NORMAL)
    }

    companion object {
      /**
       * [Pattern] which will be used for finding some information in the sender name
       */
      val SENDER_NAME_PATTERN: Pattern =
        Pattern.compile(StringBuilder().apply {
          val domains = ArrayList<String>()
          domains.add(JavaEmailConstants.EMAIL_PROVIDER_GMAIL)
          domains.add(JavaEmailConstants.EMAIL_PROVIDER_YAHOO)
          domains.add(JavaEmailConstants.EMAIL_PROVIDER_LIVE)
          domains.add(JavaEmailConstants.EMAIL_PROVIDER_OUTLOOK)

          append("@")
          append("(")
          append(domains[0])

          for (i in 1 until domains.size) {
            append("|")
            append(domains[i])
          }
          append(")$")
        }.toString(), Pattern.CASE_INSENSITIVE)
    }

    interface OnAvatarClickListener {
      fun onAvatarClick(msgEntity: MessageEntity)
    }

    /**
    Based on https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/
    src/com/example/android/apis/animation/Rotate3dAnimation.java#
     */
    private class Rotate3dAnimation(private val calculateCenterX: () -> Float) : Animation() {
      private val camera = Camera()

      override fun applyTransformation(interpolatedTime: Float, transformation: Transformation) {
        val rotation = 0f..180f
        val degrees = rotation.start + (rotation.endInclusive - rotation.start) * interpolatedTime

        val matrix = transformation.matrix
        val centerX = calculateCenterX()
        camera.save()
        camera.translate(0.0f, 0.0f, 0.9F * (1.0f - interpolatedTime))
        camera.rotateY(degrees)
        camera.getMatrix(matrix)
        camera.restore()

        matrix.preTranslate(-centerX, 0f)
        matrix.postTranslate(centerX, 0f)
      }

      companion object {
        private const val DURATION_IN_MILLISECONDS = 300L

        fun createAnimation(
          calculateCenterX: () -> Float
        ): Animation {
          return Rotate3dAnimation(calculateCenterX).apply {
            duration = DURATION_IN_MILLISECONDS
            interpolator = FastOutSlowInInterpolator()
          }
        }
      }
    }
  }

  interface OnMessageClickListener {
    fun onMsgClick(position: Int, msgEntity: MessageEntity)
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessageEntity>() {
      override fun areItemsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg.id == newMsg.id

      override fun areContentsTheSame(oldMsg: MessageEntity, newMsg: MessageEntity) =
        oldMsg == newMsg
    }

    private const val MAX_LABELS_COUNT_BE_VISIBLE = 2

    @IntDef(NONE, FOOTER, MESSAGE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ItemType

    const val NONE = 0
    const val FOOTER = 1
    const val MESSAGE = 2
  }
}
