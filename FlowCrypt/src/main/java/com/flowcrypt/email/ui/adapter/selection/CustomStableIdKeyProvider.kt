/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.ui.adapter.MsgsPagedListAdapter

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

  /**
   * This method is important for selection stability.
   * If it returns a wrong position the selection will disappear during scrolling.
   */
  override fun getPosition(key: Long): Int {
    val adapter = recyclerView.adapter as? MsgsPagedListAdapter ?: return RecyclerView.NO_POSITION
    return adapter.currentList?.indexOfFirst { it?.id == key } ?: RecyclerView.NO_POSITION
  }
}
