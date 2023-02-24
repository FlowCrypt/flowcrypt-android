/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.adapter.PrivateKeysRecyclerViewAdapter

/**
 * @author Denys Bondarenko
 */
class PrivateKeyItemDetailsLookup(private val recyclerView: RecyclerView) :
  ItemDetailsLookup<PgpKeyDetails>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<PgpKeyDetails>? {
    return recyclerView.findChildViewUnder(e.x, e.y)?.let {
      (recyclerView.getChildViewHolder(it) as? PrivateKeysRecyclerViewAdapter.ViewHolder)?.getPgpKeyDetails()
    }
  }
}
