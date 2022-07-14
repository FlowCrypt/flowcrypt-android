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
import com.flowcrypt.email.extensions.visibleOrGone

/**
 * @author Denis Bondarenko
 *         Date: 7/14/22
 *         Time: 11:13 AM
 *         E-mail: DenBond7@gmail.com
 */
class AutoCompleteResultRecyclerViewAdapter(
  private val resultListener: OnResultListener
) : ListAdapter<RecipientWithPubKeys,
    AutoCompleteResultRecyclerViewAdapter.BaseViewHolder>(DIFF_CALLBACK) {
  private val alreadyAddedRecipientsSet = mutableSetOf<String>()

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

  fun submitList(
    list: List<RecipientWithPubKeys>?,
    alreadyAddedRecipientsSet: Set<String>
  ) {
    this.alreadyAddedRecipientsSet.clear()
    this.alreadyAddedRecipientsSet.addAll(alreadyAddedRecipientsSet)
    submitList(list)
  }

  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

  inner class ResultViewHolder(itemView: View) : BaseViewHolder(itemView) {
    private val binding: RecipientAutoCompleteItemBinding =
      RecipientAutoCompleteItemBinding.bind(itemView)

    fun bind(recipientWithPubKeys: RecipientWithPubKeys) {
      itemView.setOnClickListener {
        resultListener.onResultClick(recipientWithPubKeys)
        submitList(null)
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

      binding.textViewUsed.visibleOrGone(recipientWithPubKeys.recipient.email in alreadyAddedRecipientsSet)
    }
  }

  interface OnResultListener {
    fun onResultClick(recipientWithPubKeys: RecipientWithPubKeys)
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

    @IntDef(ADD, ITEM)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    const val ADD = 0
    const val ITEM = 1
  }
}
