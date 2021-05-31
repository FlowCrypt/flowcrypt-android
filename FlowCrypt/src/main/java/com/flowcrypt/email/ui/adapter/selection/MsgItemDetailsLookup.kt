/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.ui.adapter.MsgsPagedListAdapter

/**
 * @author Denis Bondarenko
 *         Date: 1/5/20
 *         Time: 2:48 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgItemDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
    return recyclerView.findChildViewUnder(e.x, e.y)?.let {
      (recyclerView.getChildViewHolder(it) as? MsgsPagedListAdapter.BaseViewHolder)?.getItemDetails()
    }
  }
}
