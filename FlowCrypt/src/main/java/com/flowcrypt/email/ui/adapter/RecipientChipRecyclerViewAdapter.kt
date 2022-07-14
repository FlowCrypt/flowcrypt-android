/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.database.Cursor
import android.text.TextUtils
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
import com.flowcrypt.email.databinding.ComposeAddRecipientItemBinding
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.extensions.toast
import com.google.android.material.chip.Chip
import jakarta.mail.Message


/**
 * @author Denis Bondarenko
 *         Date: 7/7/22
 *         Time: 5:35 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientChipRecyclerViewAdapter(
  var showGroupEnabled: Boolean = false,
  val anchorResId: Int,
  private val onChipsListener: OnChipsListener
) :
  ListAdapter<RecipientChipRecyclerViewAdapter.RecipientInfo, RecipientChipRecyclerViewAdapter.BaseViewHolder>(
    DIFF_CALLBACK
  ) {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecipientChipRecyclerViewAdapter.BaseViewHolder {
    val chip = Chip(parent.context)

    return when (viewType) {
      ADD -> AddViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.compose_add_recipient_item, parent, false)
      )
      else -> ChipViewHolder(chip.apply { textSize = 16f })
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
    private val binding: ComposeAddRecipientItemBinding =
      ComposeAddRecipientItemBinding.bind(itemView)

    fun bind() {
      binding.autoCompleteTextViewEmailAddress.addTextChangedListener { editable ->
        editable?.let { onChipsListener.onEmailAddressTyped(it) }
      }

      binding.autoCompleteTextViewEmailAddress.setOnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
          onChipsListener.onEmailAddressTyped("")
        }
      }

      binding.autoCompleteTextViewEmailAddress.setOnEditorActionListener { v, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
          EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT -> {
            if (v.text.toString().isValidEmail()) {
              onChipsListener.onEmailAddressAdded(v.text)
              v.text = null
              false
            } else {
              v.context.toast(
                text = v.context.getString(R.string.type_valid_email_or_select_from_dropdown)
              )
              true
            }
          }
          else -> false
        }
      }

      binding.autoCompleteTextViewEmailAddress.setOnItemClickListener { parent, _, position, _ ->
        val adapter = parent.adapter as? RecipientAdapter
        val selectedItem = adapter?.getItem(position) as? Cursor
        selectedItem?.let { item ->
          onChipsListener.onEmailAddressAdded(
            adapter.convertToString(item)
          )
        }
        binding.autoCompleteTextViewEmailAddress.text = null
      }
    }
  }

  inner class ChipViewHolder(itemView: View) : BaseViewHolder(itemView) {
    fun bind(recipientInfo: RecipientInfo) {
      val chip = itemView as Chip
      chip.ellipsize = TextUtils.TruncateAt.MIDDLE
      chip.text = recipientInfo.recipientWithPubKeys.recipient.name
        ?: recipientInfo.recipientWithPubKeys.recipient.email
      chip.isCloseIconVisible = true

      /*val progressIndicatorSpec = CircularProgressIndicatorSpec(
        itemView.context,
        null,
        0,
        R.style.Widget_MaterialComponents_CircularProgressIndicator_ExtraSmall
      )

      progressIndicatorSpec.indicatorInset = 1

      chip.chipIcon =
        IndeterminateDrawable.createCircularDrawable(itemView.context, progressIndicatorSpec)*/

      chip.setOnCloseIconClickListener {
        onChipsListener.onChipDeleted(recipientInfo)
      }
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
