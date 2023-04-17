/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.IntDef
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.databinding.ChipMoreItemBinding
import com.flowcrypt.email.databinding.ChipRecipientItemBinding
import com.flowcrypt.email.databinding.ComposeAddRecipientItemBinding
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import jakarta.mail.Message


/**
 * @author Denys Bondarenko
 */
class RecipientChipRecyclerViewAdapter(
  val recipientType: Message.RecipientType,
  private val onChipsListener: OnChipsListener
) : ListAdapter<RecipientChipRecyclerViewAdapter.Item,
    RecipientChipRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {
  private var addViewHolder: AddViewHolder? = null

  var resetTypedText = false
    set(value) {
      field = value
      if (value) {
        addViewHolder?.binding?.editTextEmailAddress?.text = null
      }
    }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecipientChipRecyclerViewAdapter.BaseViewHolder {
    return when (viewType) {
      ADD -> {
        addViewHolder = AddViewHolder(
          LayoutInflater.from(parent.context)
            .inflate(R.layout.compose_add_recipient_item, parent, false)
        )
        requireNotNull(addViewHolder)
      }

      MORE -> MoreViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.chip_more_item, parent, false)
      )

      else -> ChipViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.chip_recipient_item, parent, false)
      )
    }
  }

  override fun onBindViewHolder(
    holder: RecipientChipRecyclerViewAdapter.BaseViewHolder,
    position: Int
  ) {
    when (holder) {
      is AddViewHolder -> holder.bind()
      is ChipViewHolder -> holder.bind(getItem(position).itemData as RecipientInfo)
      is MoreViewHolder -> holder.bind(getItem(position).itemData as ItemData.More)
    }
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position).type
  }

  fun submitList(recipients: Map<String, RecipientInfo>, isModifyingEnabled: Boolean = true) {
    val filteredList = recipients.values
      .take(if (hasInputFocus()) recipients.size else MAX_VISIBLE_ITEMS_COUNT)
    val hasUpdatingInHiddenItems = (recipients.values - filteredList.toSet()).any { it.isUpdating }
    val recipientInfoList = filteredList
      .map { Item(CHIP, it.copy(isModifyingEnabled = isModifyingEnabled)) }
    val finalList = recipientInfoList.toMutableList().apply {
      if (recipients.size > MAX_VISIBLE_ITEMS_COUNT && !hasInputFocus()) {
        add(
          Item(
            MORE,
            ItemData.More(
              value = recipients.size - filteredList.size,
              hasUpdatingInHiddenItems = hasUpdatingInHiddenItems
            )
          )
        )
      }
      if (isModifyingEnabled) {
        add(Item(ADD, ItemData.ADD))
      }
    }
    submitList(finalList)
  }

  fun requestFocus() {
    addViewHolder?.binding?.editTextEmailAddress?.requestFocus()
  }

  private fun hasInputFocus(): Boolean {
    return addViewHolder?.binding?.editTextEmailAddress?.hasFocus() == true
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun prepareProgressDrawable(): Drawable {
      return CircularProgressDrawable(itemView.context).apply {
        setStyle(CircularProgressDrawable.DEFAULT)
        colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          UIUtil.getColor(itemView.context, R.color.colorPrimary), BlendModeCompat.SRC_IN
        )
        start()
      }
    }
  }

  inner class AddViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ComposeAddRecipientItemBinding.bind(itemView)

    fun bind() {
      binding.editTextEmailAddress.addTextChangedListener { editable ->
        editable?.let { onChipsListener.onEmailAddressTyped(recipientType, it) }
      }

      binding.editTextEmailAddress.setOnFocusChangeListener { _, hasFocus ->
        onChipsListener.onAddFieldFocusChanged(recipientType, hasFocus)
        if (!hasFocus) {
          binding.editTextEmailAddress.text = null
          onChipsListener.onEmailAddressTyped(recipientType, "")
        }
      }

      binding.editTextEmailAddress.setOnEditorActionListener { v, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
          EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT -> {
            if (v.text.toString().isValidEmail()) {
              onChipsListener.onEmailAddressAdded(recipientType, v.text)
              v.text = null
              false
            } else {
              v.context.toast(v.context.getString(R.string.type_valid_email_or_select_from_dropdown))
              true
            }
          }
          else -> false
        }
      }
    }
  }

  inner class ChipViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ChipRecipientItemBinding.bind(itemView)
    fun bind(recipientInfo: RecipientInfo) {
      val chip = binding.chip
      chip.text = recipientInfo.recipientWithPubKeys.recipient.name
        ?: recipientInfo.recipientWithPubKeys.recipient.email

      updateChipBackgroundColor(chip, recipientInfo)
      updateChipTextColor(chip, recipientInfo)
      updateChipIcon(chip, recipientInfo)

      chip.isCloseIconVisible = recipientInfo.isModifyingEnabled
      chip.setOnCloseIconClickListener {
        onChipsListener.onChipDeleted(recipientType, recipientInfo)
      }
    }

    private fun updateChipBackgroundColor(chip: Chip, recipientInfo: RecipientInfo) {
      val recipientWithPubKeys = recipientInfo.recipientWithPubKeys

      val color = when {
        recipientInfo.isUpdating -> {
          MaterialColors.getColor(
            chip.context,
            com.google.android.material.R.attr.colorSurface,
            Color.WHITE
          )
        }

        recipientWithPubKeys.hasAtLeastOnePubKey() -> {
          val colorResId = when {
            !recipientWithPubKeys.hasUsablePubKey() -> CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY
            !recipientWithPubKeys.hasNotRevokedPubKey() -> CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED
            !recipientWithPubKeys.hasNotExpiredPubKey() -> CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED
            else -> CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
          }
          UIUtil.getColor(chip.context, colorResId)
        }

        else -> {
          UIUtil.getColor(chip.context, CHIP_COLOR_RES_ID_NO_PUB_KEY)
        }
      }

      chip.chipBackgroundColor = ColorStateList.valueOf(color)
    }

    private fun updateChipTextColor(chip: Chip, recipientInfo: RecipientInfo) {
      val color = when {
        recipientInfo.isUpdating -> {
          MaterialColors.getColor(
            chip.context,
            com.google.android.material.R.attr.colorOnSurface,
            Color.BLACK
          )
        }

        else -> {
          MaterialColors.getColor(
            chip.context,
            com.google.android.material.R.attr.colorOnSurfaceInverse,
            Color.WHITE
          )
        }
      }

      chip.setTextColor(color)
    }

    private fun updateChipIcon(chip: Chip, recipientInfo: RecipientInfo) {
      chip.chipIcon = if (recipientInfo.isUpdating) prepareProgressDrawable() else null
    }
  }

  inner class MoreViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ChipMoreItemBinding.bind(itemView)

    fun bind(more: ItemData.More) {
      binding.chipMore.text = itemView.context.getString(R.string.more_recipients, more.value)
      binding.chipMore.chipIcon =
        if (more.hasUpdatingInHiddenItems) prepareProgressDrawable() else null
      itemView.setOnClickListener {
        addViewHolder?.binding?.editTextEmailAddress?.requestFocus()
      }
    }
  }

  interface OnChipsListener {
    fun onEmailAddressTyped(recipientType: Message.RecipientType, email: CharSequence)
    fun onEmailAddressAdded(recipientType: Message.RecipientType, email: CharSequence)
    fun onChipDeleted(recipientType: Message.RecipientType, recipientInfo: RecipientInfo)
    fun onAddFieldFocusChanged(recipientType: Message.RecipientType, hasFocus: Boolean)
  }

  data class RecipientInfo(
    val recipientType: Message.RecipientType,
    val recipientWithPubKeys: RecipientWithPubKeys,
    val creationTime: Long = System.currentTimeMillis(),
    var isUpdating: Boolean = true,
    var isUpdateFailed: Boolean = false,
    val isModifyingEnabled: Boolean = true
  ) : ItemData {
    override val uniqueId: Long = requireNotNull(recipientWithPubKeys.recipient.id)
  }

  data class Item(@Type val type: Int, val itemData: ItemData)

  interface ItemData {
    val uniqueId: Long

    data class More(
      val value: Int,
      val hasUpdatingInHiddenItems: Boolean,
      override val uniqueId: Long = Long.MAX_VALUE
    ) : ItemData

    companion object {
      val ADD = object : ItemData {
        override val uniqueId: Long
          get() = Long.MIN_VALUE
      }
    }
  }

  companion object {
    val CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY = R.color.colorPrimary
    val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED = R.color.orange
    val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED = R.color.red
    val CHIP_COLOR_RES_ID_NO_PUB_KEY = R.color.gray
    val CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY = R.color.red
    private const val MAX_VISIBLE_ITEMS_COUNT = 3

    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
      override fun areItemsTheSame(old: Item, new: Item) =
        old.itemData.uniqueId == new.itemData.uniqueId

      override fun areContentsTheSame(old: Item, new: Item): Boolean = old == new
    }

    @IntDef(CHIP, ADD, MORE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    const val CHIP = 0
    const val ADD = 1
    const val MORE = 2
  }
}
