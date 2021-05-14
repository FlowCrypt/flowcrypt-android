/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.security.model.NodeKeyDetails
import com.flowcrypt.email.ui.adapter.PrivateKeysRecyclerViewAdapter

/**
 * @author Denis Bondarenko
 *         Date: 11/2/20
 *         Time: 4:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class PrivateKeyItemDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<NodeKeyDetails>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<NodeKeyDetails>? {
    return recyclerView.findChildViewUnder(e.x, e.y)?.let {
      (recyclerView.getChildViewHolder(it) as? PrivateKeysRecyclerViewAdapter.ViewHolder)?.getNodeKeyDetails()
    }
  }
}