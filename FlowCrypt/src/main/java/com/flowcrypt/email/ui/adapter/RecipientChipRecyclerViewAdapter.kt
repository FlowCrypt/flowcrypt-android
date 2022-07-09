/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.database.Cursor
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.google.android.material.chip.Chip


/**
 * @author Denis Bondarenko
 *         Date: 7/7/22
 *         Time: 5:35 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientChipRecyclerViewAdapter(
  var showGroupEnabled: Boolean = false,
  private val recipientAdapter: RecipientAdapter
) :
  ListAdapter<RecipientWithPubKeys, RecipientChipRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecipientChipRecyclerViewAdapter.BaseViewHolder {
    val chip = Chip(parent.context)

    return when (viewType) {
      ADD -> AddViewHolder(AutoCompleteTextView(parent.context).apply {
        dropDownAnchor = R.id.rVChips
        threshold = 2
        inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        setAdapter(recipientAdapter)
        minWidth = resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
        textSize = 16f
        setBackgroundColor(android.R.color.transparent)
        setOnItemClickListener { parent, view, position, id ->
          val adapter = parent.adapter as RecipientAdapter
          val s = adapter.getItem(position) as Cursor
          Toast.makeText(context, "s = ${adapter.convertToString(s)}", Toast.LENGTH_SHORT).show()
        }
      })
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

  inner class AddViewHolder(private val autoCompleteTextView: AutoCompleteTextView) :
    BaseViewHolder(autoCompleteTextView) {
    fun bind() {

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

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipientWithPubKeys>() {
      override fun areItemsTheSame(old: RecipientWithPubKeys, new: RecipientWithPubKeys): Boolean {
        return old.recipient.id == new.recipient.id
      }

      override fun areContentsTheSame(
        old: RecipientWithPubKeys, new: RecipientWithPubKeys
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
