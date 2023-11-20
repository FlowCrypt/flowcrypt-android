/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.ui.adapter.PrivateKeysListAdapter
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
class KeyRingInfoItemDetailsLookup(private val recyclerView: RecyclerView) :
  ItemDetailsLookup<KeyRingInfo>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<KeyRingInfo>? {
    return recyclerView.findChildViewUnder(e.x, e.y)?.let {
      (recyclerView.getChildViewHolder(it) as? PrivateKeysListAdapter.ViewHolder)?.getItemDetails()
    }
  }
}
