/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

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
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone

/**
 * @author Denis Bondarenko
 *         Date: 7/14/22
 *         Time: 11:13 AM
 *         E-mail: DenBond7@gmail.com
 */
class AutoCompleteResultRecyclerViewAdapter(
  private val resultListener: OnResultListener
) : ListAdapter<AutoCompleteResultRecyclerViewAdapter.AutoCompleteItem,
    AutoCompleteResultRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AutoCompleteResultRecyclerViewAdapter.BaseViewHolder {
    return ResultViewHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.recipient_auto_complete_item, parent, false)
    )
  }

  override fun onBindViewHolder(
    holder: AutoCompleteResultRecyclerViewAdapter.BaseViewHolder,
    position: Int
  ) {
    val item = getItem(position)
    (holder as ResultViewHolder).bind(item)
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

  inner class ResultViewHolder(itemView: View) : BaseViewHolder(itemView) {
    private val binding: RecipientAutoCompleteItemBinding =
      RecipientAutoCompleteItemBinding.bind(itemView)

    fun bind(autoCompleteItem: AutoCompleteItem) {
      val recipientWithPubKeys = autoCompleteItem.recipientWithPubKeys
      itemView.setOnClickListener {
        if (autoCompleteItem.isAdded) {
          itemView.context.toast(itemView.context.getString(R.string.already_added))
        } else {
          resultListener.onResultClick(recipientWithPubKeys)
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
    fun onResultClick(recipientWithPubKeys: RecipientWithPubKeys)
  }

  data class AutoCompleteItem(val isAdded: Boolean, val recipientWithPubKeys: RecipientWithPubKeys)

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
