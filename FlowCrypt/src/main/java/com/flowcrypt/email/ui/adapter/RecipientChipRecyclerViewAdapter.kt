/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.IntDef
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.databinding.ChipRecipientItemBinding
import com.flowcrypt.email.databinding.ComposeAddRecipientItemBinding
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import jakarta.mail.Message


/**
 * @author Denis Bondarenko
 *         Date: 7/7/22
 *         Time: 5:35 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientChipRecyclerViewAdapter(
  var showGroupEnabled: Boolean = false,
  private val onChipsListener: OnChipsListener
) : ListAdapter<RecipientChipRecyclerViewAdapter.RecipientInfo,
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
        if (addViewHolder == null) {
          addViewHolder = AddViewHolder(
            LayoutInflater.from(parent.context)
              .inflate(R.layout.compose_add_recipient_item, parent, false)
          )
        }

        requireNotNull(addViewHolder)
      }
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
      is ChipViewHolder -> holder.bind(getItem(position))
    }
  }

  override fun getItemCount(): Int {
    return super.getItemCount() + if (showGroupEnabled) 2 else 1
  }

  override fun getItemViewType(position: Int): Int {
    return when (position) {
      itemCount - 1 -> ADD
      else -> CHIP
    }
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

  inner class AddViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ComposeAddRecipientItemBinding.bind(itemView)

    fun bind() {
      binding.editTextEmailAddress.addTextChangedListener { editable ->
        editable?.let { onChipsListener.onEmailAddressTyped(it) }
      }

      binding.editTextEmailAddress.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
          binding.editTextEmailAddress.text = null
          onChipsListener.onEmailAddressTyped("")
        }
      }

      binding.editTextEmailAddress.setOnEditorActionListener { v, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
          EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT -> {
            if (v.text.toString().isValidEmail()) {
              onChipsListener.onEmailAddressAdded(v.text)
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

      chip.setOnCloseIconClickListener {
        onChipsListener.onChipDeleted(recipientInfo)
      }
    }

    private fun updateChipBackgroundColor(chip: Chip, recipientInfo: RecipientInfo) {
      val recipientWithPubKeys = recipientInfo.recipientWithPubKeys

      val color = when {
        recipientInfo.isUpdating -> {
          MaterialColors.getColor(chip.context, R.attr.colorSurface, Color.WHITE)
        }

        recipientWithPubKeys.hasAtLeastOnePubKey() -> {
          val colorResId = when {
            !recipientWithPubKeys.hasUsablePubKey() -> CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY
            recipientWithPubKeys.hasNotRevokedPubKey() -> CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED
            recipientWithPubKeys.hasNotExpiredPubKey() -> CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED
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
          MaterialColors.getColor(chip.context, R.attr.colorOnSurface, Color.BLACK)
        }

        else -> {
          MaterialColors.getColor(chip.context, R.attr.colorOnSurfaceInverse, Color.WHITE)
        }
      }

      chip.setTextColor(color)
    }

    private fun updateChipIcon(chip: Chip, recipientInfo: RecipientInfo) {
      if (recipientInfo.isUpdating) {
        chip.chipIcon = prepareProgressDrawable()
      } else {
        chip.chipIcon = null
      }
    }

    private fun prepareProgressDrawable(): IndeterminateDrawable<CircularProgressIndicatorSpec> {
      val progressIndicatorSpec = CircularProgressIndicatorSpec(
        itemView.context,
        null,
        0,
        R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall
      ).apply {
        indicatorInset = 1
      }
      return IndeterminateDrawable.createCircularDrawable(itemView.context, progressIndicatorSpec)
    }
  }

  interface OnChipsListener {
    fun onEmailAddressTyped(email: CharSequence)
    fun onEmailAddressAdded(email: CharSequence)
    fun onChipDeleted(recipientInfo: RecipientInfo)
  }

  data class RecipientInfo(
    val recipientType: Message.RecipientType,
    val recipientWithPubKeys: RecipientWithPubKeys,
    val creationTime: Long = System.currentTimeMillis(),
    var isUpdating: Boolean = true,
    var isUpdateFailed: Boolean = false
  )

  companion object {
    const val CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY = R.color.colorPrimary
    const val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED = R.color.orange
    const val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED = R.color.red
    const val CHIP_COLOR_RES_ID_NO_PUB_KEY = R.color.gray
    const val CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY = R.color.red

    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipientInfo>() {
      override fun areItemsTheSame(old: RecipientInfo, new: RecipientInfo): Boolean {
        return old.recipientWithPubKeys.recipient.id == new.recipientWithPubKeys.recipient.id
      }

      override fun areContentsTheSame(
        old: RecipientInfo,
        new: RecipientInfo
      ): Boolean {
        return old == new
      }
    }

    @IntDef(CHIP, ADD, COUNT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    const val CHIP = 0
    const val ADD = 1
    const val COUNT = 2
  }
}
