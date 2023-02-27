/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

/**
 * @author Denys Bondarenko
 */
class CustomStableIdKeyProvider(private val recyclerView: RecyclerView) :
  ItemKeyProvider<Long>(SCOPE_CACHED) {
  init {
    requireNotNull(recyclerView.adapter)
    require(recyclerView.adapter?.hasStableIds() == true) {
      "Adapter should have stable ids"
    }
  }

  override fun getKey(position: Int): Long? {
    return recyclerView.adapter?.getItemId(position)
  }

  override fun getPosition(key: Long): Int {
    return recyclerView.findViewHolderForItemId(key)?.bindingAdapterPosition
      ?: RecyclerView.NO_POSITION
  }
}
