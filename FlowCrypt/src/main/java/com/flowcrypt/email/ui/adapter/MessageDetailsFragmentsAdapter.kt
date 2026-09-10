/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragmentArgs

/**
 * @author Denys Bondarenko
 */
class MessageDetailsFragmentsAdapter(
  private val localFolder: LocalFolder,
  initialList: List<MessageEntity>,
  fragment: Fragment,
  listListener: AsyncListDiffer.ListListener<MessageEntity>
) : FragmentStateAdapter(fragment) {
  private val diffUtil = object : DiffUtil.ItemCallback<MessageEntity>() {
    override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity):
        Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity):
        Boolean {
      return oldItem == newItem
    }
  }

  private val asyncListDiffer = AsyncListDiffer(this, diffUtil)

  init {
    asyncListDiffer.submitList(initialList)
    asyncListDiffer.addListListener(listListener)
  }

  override fun getItemCount(): Int = asyncListDiffer.currentList.size

  override fun createFragment(position: Int): Fragment =
    MessageDetailsFragment().apply {
      arguments = MessageDetailsFragmentArgs(
        messageEntity = asyncListDiffer.currentList[position],
        localFolder = localFolder,
        isViewPagerMode = true,
      ).toBundle()
    }

  override fun getItemId(position: Int): Long {
    return asyncListDiffer.currentList[position].id ?: 0
  }

  override fun containsItem(itemId: Long): Boolean {
    return asyncListDiffer.currentList.any { it.id == itemId }
  }

  fun submit(newList: List<MessageEntity>) {
    asyncListDiffer.submitList(newList)
  }

  fun getItem(position: Int): MessageEntity? {
    return asyncListDiffer.currentList.getOrNull(position)
  }
}