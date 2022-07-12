/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.databinding.ComposeAddRecipientItemBinding
import com.google.android.material.chip.Chip


/**
 * @author Denis Bondarenko
 *         Date: 7/7/22
 *         Time: 5:35 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientChipRecyclerViewAdapter(
  var showGroupEnabled: Boolean = false,
  private val onChipsListener: OnChipsListener
) :
  ListAdapter<RecipientWithPubKeys, RecipientChipRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {
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
      binding.editTextTextEmailAddress.addTextChangedListener {
        onChipsListener.onEmailAddressTyped(it)
      }
    }
  }

  inner class ChipViewHolder(itemView: View) : BaseViewHolder(itemView) {
    fun bind(recipientWithPubKeys: RecipientWithPubKeys) {
      val chip = itemView as Chip
      chip.text = recipientWithPubKeys.recipient.email
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
        Toast.makeText(
          itemView.context,
          recipientWithPubKeys.recipient.email,
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  interface OnChipsListener {
    fun onEmailAddressTyped(email: Editable?)
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipientWithPubKeys>() {
      override fun areItemsTheSame(old: RecipientWithPubKeys, new: RecipientWithPubKeys): Boolean {
        return old.recipient.id == new.recipient.id
      }

      override fun areContentsTheSame(
        old: RecipientWithPubKeys,
        new: RecipientWithPubKeys
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
