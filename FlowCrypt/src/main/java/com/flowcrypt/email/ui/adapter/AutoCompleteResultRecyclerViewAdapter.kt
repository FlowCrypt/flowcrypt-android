/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.databinding.RecipientAutoCompleteItemBinding
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import jakarta.mail.Message

/**
 * @author Denys Bondarenko
 */
class AutoCompleteResultRecyclerViewAdapter(
  val recipientType: Message.RecipientType,
  private val resultListener: OnResultListener
) : ListAdapter<AutoCompleteResultRecyclerViewAdapter.AutoCompleteItem,
    AutoCompleteResultRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {

  init {
    setHasStableIds(true)
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position).type
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AutoCompleteResultRecyclerViewAdapter.BaseViewHolder {
    return when (viewType) {
      ADD -> AddViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.recipient_auto_complete_item, parent, false)
      )
      else -> ResultViewHolder(
        LayoutInflater.from(parent.context)
          .inflate(R.layout.recipient_auto_complete_item, parent, false)
      )
    }
  }

  override fun getItemId(position: Int): Long {
    return when (getItem(position).type) {
      ADD -> Long.MAX_VALUE
      else -> requireNotNull(getItem(position).recipientWithPubKeys.recipient.id)
    }
  }

  override fun onBindViewHolder(
    holder: AutoCompleteResultRecyclerViewAdapter.BaseViewHolder,
    position: Int
  ) {
    when (holder) {
      is AddViewHolder -> holder.bind(getItem(position))
      is ResultViewHolder -> holder.bind(getItem(position))
    }
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

  inner class AddViewHolder(itemView: View) : BaseViewHolder(itemView) {
    private val binding: RecipientAutoCompleteItemBinding =
      RecipientAutoCompleteItemBinding.bind(itemView)

    fun bind(autoCompleteItem: AutoCompleteItem) {
      val context = itemView.context
      val typedText = autoCompleteItem.recipientWithPubKeys.recipient.email
      itemView.setOnClickListener {
        if (typedText.isValidEmail()) {
          resultListener.onResultClick(recipientType, autoCompleteItem.recipientWithPubKeys)
          submitList(null)
        } else {
          context.toast(context.getString(R.string.type_valid_email_or_select_from_dropdown))
        }
      }

      binding.imageViewPgp.setImageResource(R.drawable.ic_outline_add_circle_outline_32)
      binding.textViewEmail.typeface = Typeface.DEFAULT_BOLD
      binding.textViewEmail.text = autoCompleteItem.recipientWithPubKeys.recipient.email
      binding.textViewName.typeface = Typeface.DEFAULT
      binding.textViewName.text = context.getString(R.string.add_recipient)
    }
  }

  inner class ResultViewHolder(itemView: View) : BaseViewHolder(itemView) {
    private val binding: RecipientAutoCompleteItemBinding =
      RecipientAutoCompleteItemBinding.bind(itemView)

    fun bind(autoCompleteItem: AutoCompleteItem) {
      val recipientWithPubKeys = autoCompleteItem.recipientWithPubKeys
      itemView.setOnClickListener {
        if (autoCompleteItem.isAdded) {
          itemView.context.toast(itemView.context.getString(R.string.already_added))
        } else {
          resultListener.onResultClick(recipientType, recipientWithPubKeys)
          submitList(null)
        }
      }

      binding.textViewEmail.text = recipientWithPubKeys.recipient.email
      binding.textViewName.text = recipientWithPubKeys.recipient.name
      binding.textViewName.visibleOrGone(recipientWithPubKeys.recipient.name?.isNotEmpty() == true)

      binding.imageViewPgp.setColorFilter(
        ContextCompat.getColor(
          itemView.context,
          if (recipientWithPubKeys.hasUsablePubKey()) R.color.colorPrimary else R.color.gray
        ), android.graphics.PorterDuff.Mode.SRC_IN
      )

      binding.textViewUsed.visibleOrGone(autoCompleteItem.isAdded)
    }
  }

  interface OnResultListener {
    fun onResultClick(
      recipientType: Message.RecipientType,
      recipientWithPubKeys: RecipientWithPubKeys
    )
  }

  data class AutoCompleteItem(
    val isAdded: Boolean,
    val recipientWithPubKeys: RecipientWithPubKeys,
    val type: Int = ITEM
  )

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AutoCompleteItem>() {
      override fun areItemsTheSame(old: AutoCompleteItem, new: AutoCompleteItem): Boolean {
        return old.recipientWithPubKeys.recipient.id == new.recipientWithPubKeys.recipient.id
      }

      override fun areContentsTheSame(
        old: AutoCompleteItem,
        new: AutoCompleteItem
      ): Boolean {
        return old == new
      }
    }

    @IntDef(ADD, ITEM)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    const val ADD = 0
    const val ITEM = 1
  }
}
