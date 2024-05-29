/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.databinding.ItemGmailLabelWithCheckboxBinding
import com.flowcrypt.email.extensions.kotlin.parseAsColorBasedOnDefaultSettings
import com.flowcrypt.email.model.LabelWithChoice
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * @author Denys Bondarenko
 */
class GmailApiLabelsWithChoiceListAdapter :
  ListAdapter<LabelWithChoice, GmailApiLabelsWithChoiceListAdapter.ViewHolder>(
    DiffUtilCallBack()
  ) {
  //we use a separate value to store modified states to prevent UI blinking after 'submit' method.
  private val statesSessionMap = mutableMapOf<String, Int>()

  private val onLabelCheckedListener = object : OnLabelCheckedListener {
    override fun onLabelChecked(labelWithChoice: LabelWithChoice, state: Int) {
      statesSessionMap[labelWithChoice.id] = state
    }
  }

  override fun submitList(list: List<LabelWithChoice>?) {
    super.submitList(list)
    statesSessionMap.clear()
    list?.let { statesSessionMap.putAll(list.associateBy({ it.id }, { it.state })) }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.item_gmail_label_with_checkbox, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position), onLabelCheckedListener)
  }

  fun hasChanges(): Boolean {
    return statesSessionMap != currentList.associateBy({ it.id }, { it.state })
  }

  fun getActualListWithModifications(): List<LabelWithChoice> {
    return currentList.map { it.copy(state = statesSessionMap[it.id] ?: it.state) }
  }

  inner class ViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val binding = ItemGmailLabelWithCheckboxBinding.bind(itemView)

    fun bindTo(item: LabelWithChoice, onLabelCheckedListener: OnLabelCheckedListener) {
      itemView.setOnClickListener { binding.checkBox.toggle() }
      binding.textViewLabel.text = item.name
      (binding.checkBox as? MaterialCheckBox)?.clearOnCheckedStateChangedListeners()
      (binding.checkBox as? MaterialCheckBox)?.checkedState =
        statesSessionMap[item.id] ?: MaterialCheckBox.STATE_UNCHECKED
      (binding.checkBox as? MaterialCheckBox)?.addOnCheckedStateChangedListener { checkBox, state ->
        val finalState = if (state == MaterialCheckBox.STATE_CHECKED
          && statesSessionMap[item.id] == MaterialCheckBox.STATE_UNCHECKED
          && item.initialState == MaterialCheckBox.STATE_INDETERMINATE
        ) {
          checkBox.checkedState = MaterialCheckBox.STATE_INDETERMINATE
          MaterialCheckBox.STATE_INDETERMINATE
        } else {
          state
        }

        onLabelCheckedListener.onLabelChecked(
          labelWithChoice = item,
          state = finalState
        )
      }

      val folderIconResourceId = R.drawable.ic_label_24dp
      when {
        item.name == GmailApiHelper.LABEL_INBOX -> {
          binding.imageView.setImageResource(R.drawable.ic_mail_24dp)
        }

        item.backgroundColor != null -> {
          val drawable = ContextCompat.getDrawable(itemView.context, folderIconResourceId)
          val color = item.backgroundColor.parseAsColorBasedOnDefaultSettings(itemView.context)
          drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
          )
          binding.imageView.setImageDrawable(drawable)
        }

        else -> {
          binding.imageView.setImageResource(folderIconResourceId)
        }
      }
    }
  }

  interface OnLabelCheckedListener {
    fun onLabelChecked(
      labelWithChoice: LabelWithChoice,
      @IntRange(
        from = MaterialCheckBox.STATE_UNCHECKED * 1L,
        to = MaterialCheckBox.STATE_INDETERMINATE * 1L
      )
      state: Int
    )
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<LabelWithChoice>() {
    override fun areItemsTheSame(oldItem: LabelWithChoice, newItem: LabelWithChoice): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LabelWithChoice, newItem: LabelWithChoice): Boolean {
      return oldItem == newItem
    }
  }
}
